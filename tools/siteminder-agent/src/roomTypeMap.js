// SiteMinder room name (as shown in the Reservations "Room" column, WITHOUT the "N x " prefix) -> room type
// name in sunset. This is the agent's own list: the daily run sends the sunset name, never the SiteMinder one.
// A SiteMinder name that isn't here is skipped and logged, not sent. To add a type, add a line; names are
// compared ignoring case and repeated spaces.
export const ROOM_TYPE_MAP = {
  'Sunset Room with Terrace': 'Sunset Terrace Room ABF',

  // TODO: the display names SiteMinder uses for the other sunset room types haven't been seen yet.
  // Add each one when it first shows up as "unmapped room type" in logs/agent.log.
  // '<SiteMinder name>': 'Sunset Jacuzzi Seaview Room ABF',
  // '<SiteMinder name>': 'Garden Villa ABF',
  // '<SiteMinder name>': 'Garden Jacuzzi Villa ABF',
  // '<SiteMinder name>': 'Beachfront Jacuzzi Pool Villa ABF',
  // '<SiteMinder name>': 'Beachfront Villa',
};

const key = (s) => s.trim().replace(/\s+/g, ' ').toLowerCase();

/**
 * Replaces each reservation's roomTypeName with the sunset name. Reservations whose room name isn't in the
 * map are returned separately instead of being sent.
 * @returns {{reservations: object[], unmapped: {reference: string, roomTypeName: string}[]}}
 */
export function mapRoomTypes(reservations, map = ROOM_TYPE_MAP) {
  const lookup = new Map(Object.entries(map).map(([siteMinder, sunset]) => [key(siteMinder), sunset]));
  const mapped = [];
  const unmapped = [];
  for (const r of reservations) {
    const sunset = lookup.get(key(r.roomTypeName));
    if (sunset) mapped.push({ ...r, roomTypeName: sunset });
    else unmapped.push({ reference: r.reference, roomTypeName: r.roomTypeName });
  }
  return { reservations: mapped, unmapped };
}
