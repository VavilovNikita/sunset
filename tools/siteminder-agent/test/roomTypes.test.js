import { test } from 'node:test';
import assert from 'node:assert/strict';
import { classifyRoomType } from '../src/roomTypeMap.js';
import { parseRoom } from '../src/parse.js';

// Strings quoted from what SiteMinder actually sent (the "Room" column, quantity prefix already removed by parseRoom).
const SEEN = [
  ['Jacuzzi Room with Sea View Non smoking - 484210223', 'Sunset Jacuzzi Seaview Room ABF'],
  ['Terrace Room Non smoking - 484210218', 'Sunset Terrace Room ABF'],
  ['Beachfront Jacuzzi Villa', 'Beachfront Jacuzzi Pool Villa ABF'],
  ['Sunset Room with Terrace', 'Sunset Terrace Room ABF'],
  ['Sunset Terrace Room ABF', 'Sunset Terrace Room ABF'],
];

test('strings seen from SiteMinder resolve to the right sunset room type', () => {
  for (const [seen, expected] of SEEN) assert.equal(classifyRoomType(seen), expected, seen);
});

test('the quantity prefix and numeric ids / rate suffixes do not matter', () => {
  assert.equal(classifyRoomType(parseRoom('1 x Terrace Room Non smoking - 484210218')), 'Sunset Terrace Room ABF');
  assert.equal(classifyRoomType('SUNSET   TERRACE room - Non-refundable - Breakfast included - Domestic'), 'Sunset Terrace Room ABF');
});

test('more specific rules win: jacuzzi before plain, beachfront before the rest', () => {
  assert.equal(classifyRoomType('Beachfront Villa - Domestic'), 'Beachfront Villa');
  assert.equal(classifyRoomType('Beachfront Jacuzzi Pool Villa ABF'), 'Beachfront Jacuzzi Pool Villa ABF');
  assert.equal(classifyRoomType('Garden Villa ABF'), 'Garden Villa ABF');
  assert.equal(classifyRoomType('Garden Jacuzzi Villa ABF'), 'Garden Jacuzzi Villa ABF');
  assert.equal(classifyRoomType('Jacuzzi Terrace Room'), 'Sunset Jacuzzi Terrace Room');
  assert.equal(classifyRoomType('Seaview Jacuzzi Room'), 'Sunset Jacuzzi Seaview Room ABF');
  assert.equal(classifyRoomType('Jacuzzi Room with Bathtub'), 'Sunset Jacuzzi Seaview Room ABF');
  // beachfront outranks a jacuzzi/seaview mention elsewhere in the string
  assert.equal(classifyRoomType('Beachfront Villa with Sea View'), 'Beachfront Villa');
});

test('"Sunset Jacuzzi Deluxe" is intentionally unmapped, as is anything unknown', () => {
  assert.equal(classifyRoomType('Sunset Jacuzzi Deluxe'), null);
  assert.equal(classifyRoomType('Sunset Jacuzzi Deluxe Non smoking - 484210230'), null);
  assert.equal(classifyRoomType('Mystery Bungalow'), null);
  assert.equal(classifyRoomType(''), null);
});

test('a bare "Sea View ... Bathtub" with no jacuzzi has no rule yet', () => {
  assert.equal(classifyRoomType('Sunset Sea View With Bathtub - Non-refundable - Breakfast included - Domestic'), null);
});

test('multi-room reservations are still rejected on their own, before classification', () => {
  assert.throws(() => parseRoom('2 x Terrace Room Non smoking - 484210218'), /2 rooms/);
});
