// SiteMinder's Room column is not a clean room type name. It is a rate-plan description such as
// "... Non smoking - 484210218" or "... - Non-refundable - Breakfast included - Domestic", with arbitrary
// suffixes and numeric ids. So room types are recognised by keywords, not looked up by exact name.
//
// The rules below were derived from the strings SiteMinder really sent (see test/roomTypes.test.js), NOT from
// the hotel's "Rooms and rates" catalogue, whose names do not appear in these strings.
//
// Matching is case-insensitive on the name with punctuation and repeated spaces reduced to single spaces. The
// FIRST rule that matches wins, so order matters: a more specific rule (jacuzzi) goes before the general one.
// A name no rule matches is skipped and logged as unmapped - never guessed. To support a new room type, add a
// rule; nothing else needs to change.
//
// Deliberately NOT covered: "Sunset Jacuzzi Deluxe". It is unclear which sunset room type that is (it is
// neither seaview nor terrace in its name), so it stays unmapped until a real example shows what it should be.
// "Sea View" with neither "jacuzzi" nor "bathtub", and "Bathtub"/"Jacuzzi" without sea view, are likewise unmapped.

const has = (name, ...words) => words.every((w) => name.includes(w));
const hasAny = (name, ...words) => words.some((w) => name.includes(w));

export const ROOM_TYPE_RULES = [
  { test: (n) => has(n, 'beachfront', 'jacuzzi'), result: 'Beachfront Jacuzzi Pool Villa ABF' },
  { test: (n) => has(n, 'beachfront'), result: 'Beachfront Villa' },
  { test: (n) => has(n, 'garden', 'jacuzzi'), result: 'Garden Jacuzzi Villa ABF' },
  { test: (n) => has(n, 'garden'), result: 'Garden Villa ABF' },
  // A bathtub counts as the jacuzzi variant when the string also says sea view ("Sea View With Bathtub").
  { test: (n) => hasAny(n, 'sea view', 'seaview') && hasAny(n, 'jacuzzi', 'bathtub'), result: 'Sunset Jacuzzi Seaview Room ABF' },
  { test: (n) => has(n, 'terrace', 'jacuzzi'), result: 'Sunset Jacuzzi Terrace Room' },
  { test: (n) => has(n, 'terrace'), result: 'Sunset Terrace Room ABF' },
];

const normalize = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();

/** The sunset room type for a SiteMinder room string, or null when no rule matches. */
export function classifyRoomType(siteMinderName, rules = ROOM_TYPE_RULES) {
  const n = normalize(siteMinderName);
  return rules.find((r) => r.test(n))?.result ?? null;
}

/**
 * Replaces each reservation's roomTypeName with the sunset name. Reservations no rule matches are returned
 * separately instead of being sent.
 * @returns {{reservations: object[], unmapped: {reference: string, roomTypeName: string}[]}}
 */
export function mapRoomTypes(reservations, rules = ROOM_TYPE_RULES) {
  const mapped = [];
  const unmapped = [];
  for (const r of reservations) {
    const sunset = classifyRoomType(r.roomTypeName, rules);
    if (sunset) mapped.push({ ...r, roomTypeName: sunset });
    else unmapped.push({ reference: r.reference, roomTypeName: r.roomTypeName });
  }
  return { reservations: mapped, unmapped };
}
