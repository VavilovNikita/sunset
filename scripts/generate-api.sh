#!/usr/bin/env bash
# Regenerates the OpenAPI-derived api/ and model/ classes from openapi.yaml and copies the
# result into src/main/java. This is the one authoritative command - it used to only exist as
# shell history on one machine (recovered from .claude/settings.local.json after the fact),
# which is exactly how ~20 generated files drifted from what regeneration actually produces:
# every past change hand-copied just the files it touched, using whatever flags felt right that
# day. Run this after every openapi.yaml edit, not just when a file "looks like it needs it".
#
# --template-dir openapi-templates/: the only customization on top of the stock "spring"
# generator (7.10.0) - see openapi-templates/pojo.mustache's own comment. A property marked
# `x-sensitive: true` in openapi.yaml renders as "[REDACTED]" in its model's toString() instead
# of the real value, automatically, forever - mark the field in the spec, not by hand-editing
# generated source after the fact.
#
# hideGenerationTimestamp=true: without this, the `date=` attribute in every file's `@Generated`
# annotation changes on every run even when nothing else did, so an unrelated regeneration would
# otherwise touch all ~100 files and bury the one real change in noise.
set -euo pipefail
cd "$(dirname "$0")/.."

OUT_DIR="$(mktemp -d)"
trap 'rm -rf "$OUT_DIR"' EXIT

java -jar openapi-generator-cli.jar generate \
  -i openapi.yaml \
  -g spring \
  -o "$OUT_DIR" \
  --template-dir openapi-templates \
  --api-package com.sunsetbeach.api \
  --model-package com.sunsetbeach.model \
  --invoker-package com.sunsetbeach \
  --additional-properties=interfaceOnly=true,useSpringBoot3=true,skipDefaultInterface=false,dateLibrary=java8,documentationProvider=none,useTags=true,hideGenerationTimestamp=true

DEST=src/main/java/com/sunsetbeach

# AuthApi.java: openapi.yaml tags every /auth/* operation "Auth", so the generator always
# produces this interface, but AuthController deliberately does not implement it and never has -
# login() needs an HttpServletRequest parameter (for rate-limiting by IP) that the generated
# signature has no way to carry, and register()/registerUser() would collide as two handlers on
# the same route if both existed. This is a structural limitation of expressing this one
# operation as a generated interface, not an oversight - see AuthController's own class comment.
# Skipped so it doesn't reappear as a dead, never-wired file on every future regeneration.
echo "Skipping AuthApi.java (see comment in this script for why)."

# These three models carry hand-maintained behavior a regeneration cannot express and must
# never silently overwrite:
#   - AuthResponse.java, LoginRequest.java: fully hand-written, not templated output. Custom
#     Bean Validation (LoginRequest adds @NotBlank on password, on top of the generated
#     @NotNull), and deliberately no equals()/hashCode()/toString() override at all - the
#     default Object.toString() means a stray log of one of these can never print a raw JWT or
#     password, which a generated toString() would do immediately.
#   - DeleteRoomImageRequest.java: deliberately missing @NotNull on `path`, even though
#     `required: [path]` in openapi.yaml would normally generate it. With @NotNull, Spring's
#     @Valid would reject a null `path` with this app's ValidationError shape (fieldErrors) -
#     but the spec's own documented 400 response for this operation is a plain ErrorMessage
#     ({"error": "path is required"}), which is exactly what RoomController's manual null-check
#     already produces. Restoring @NotNull would make a real request's response shape stop
#     matching the spec that documents it.
echo "Skipping AuthResponse.java, LoginRequest.java, DeleteRoomImageRequest.java (hand-maintained, see comment in this script)."

for f in "$OUT_DIR"/src/main/java/com/sunsetbeach/api/*.java; do
  base="$(basename "$f")"
  [ "$base" = "AuthApi.java" ] && continue
  cp "$f" "$DEST/api/$base"
done

for f in "$OUT_DIR"/src/main/java/com/sunsetbeach/model/*.java; do
  base="$(basename "$f")"
  case "$base" in
    AuthResponse.java|LoginRequest.java|DeleteRoomImageRequest.java) continue ;;
  esac
  cp "$f" "$DEST/model/$base"
done

# BookingCreateInput.java, UserCreateInput.java: the generator has no way to attach a custom
# Bean Validation message from openapi.yaml alone, so the hand-added
# `@Email(message = "Invalid email")` (instead of the generated bare `@Email`) gets overwritten
# by the copy above every time. Reapply it here rather than leaving it as a silent step someone
# has to remember - the sed just failing loudly (via `grep -q` first) is the safety net if the
# generated line's shape ever changes upstream.
for model in BookingCreateInput UserCreateInput; do
  file="$DEST/model/$model.java"
  if grep -q '@NotNull @jakarta.validation.constraints.Email $' "$file"; then
    sed -i 's/@NotNull @jakarta.validation.constraints.Email $/@NotNull @jakarta.validation.constraints.Email(message = "Invalid email")/' "$file"
  elif ! grep -q 'Email(message = "Invalid email")' "$file"; then
    echo "WARNING: could not reapply the custom Email validation message to $model.java - the generated line's shape changed. Check it by hand." >&2
  fi
done

echo
echo "Done. Review the diff (especially anything outside api/ and model/ that this script"
echo "didn't touch), then run the full test suite: ./mvnw test"
