import { compareMoney, formatMoney, moneyToMinor, normalizeMoney } from "./money";

describe("money utilities", () => {
  it("normalizes and formats decimal strings without floating point", () => {
    expect(normalizeMoney("1000.5")).toBe("1000.50");
    expect(formatMoney("1000.50", "EUR")).toBe("EUR 1,000.50");
    expect(moneyToMinor("9007199254740993.99")).toBe(900719925474099399n);
  });

  it("rejects exponential, negative, and over-precision values", () => {
    expect(normalizeMoney("1e3")).toBeNull();
    expect(normalizeMoney("-1.00")).toBeNull();
    expect(normalizeMoney("1.001")).toBeNull();
  });

  it("compares values through minor-unit bigint arithmetic", () => {
    expect(compareMoney("125.50", "125.49")).toBe(1);
    expect(compareMoney("0.10", "0.10")).toBe(0);
  });
});
