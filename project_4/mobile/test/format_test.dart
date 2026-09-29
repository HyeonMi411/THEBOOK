import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/utils/format.dart';

void main() {
  test('won - 천 단위 쉼표', () {
    expect(won(0), '0원');
    expect(won(1500), '1,500원');
    expect(won(1234567), '1,234,567원');
    expect(won('88000'), '88,000원');
    expect(won(null), '0원');
  });

  test('shortDateTime / shortDate', () {
    expect(shortDateTime('2026-09-29T14:30:12'), '2026-09-29 14:30');
    expect(shortDate('2026-09-29T14:30:12'), '2026-09-29');
  });

  test('imageUrl - 외부 URL 은 그대로, 서버 경로는 base 를 붙임', () {
    expect(imageUrl('https://img.test/a.png'), 'https://img.test/a.png');
    expect(imageUrl(''), '');
    expect(imageUrl('uploads/a.png'), endsWith('/uploads/a.png'));
    expect(imageUrl('/uploads/a.png'), endsWith('/uploads/a.png'));
  });

  test('asInt', () {
    expect(asInt(3), 3);
    expect(asInt('7'), 7);
    expect(asInt('x', 9), 9);
  });
}
