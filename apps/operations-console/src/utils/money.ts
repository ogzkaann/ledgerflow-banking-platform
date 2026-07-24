const MONEY_PATTERN = /^(0|[1-9]\d{0,16})(?:\.(\d{1,2}))?$/;

export function normalizeMoney(value: string): string | null {
  const trimmed = value.trim();
  const match = MONEY_PATTERN.exec(trimmed);
  if (!match) return null;
  const fraction = (match[2] ?? "").padEnd(2, "0");
  return `${match[1]}.${fraction}`;
}

export function moneyToMinor(value: string): bigint | null {
  const normalized = normalizeMoney(value);
  if (!normalized) return null;
  const [whole = "0", fraction = "00"] = normalized.split(".");
  return BigInt(whole) * 100n + BigInt(fraction);
}

export function formatMoney(value: string, currency: string): string {
  const normalized = normalizeMoney(value);
  if (!normalized) return `${value} ${currency}`;
  const [whole = "0", fraction = "00"] = normalized.split(".");
  const grouped = whole.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return `${currency} ${grouped}.${fraction}`;
}

export function compareMoney(left: string, right: string): number | null {
  const leftMinor = moneyToMinor(left);
  const rightMinor = moneyToMinor(right);
  if (leftMinor === null || rightMinor === null) return null;
  if (leftMinor === rightMinor) return 0;
  return leftMinor > rightMinor ? 1 : -1;
}
