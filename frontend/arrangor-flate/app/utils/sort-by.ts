export type SortOrder = "ascending" | "descending";

export type SortBySelector<T> = (a: T) => string | number | boolean | undefined | null;

export function sortBy<T extends object>(
  array: T[],
  order: SortOrder,
  selector: SortBySelector<T>,
): T[] {
  return array.slice().sort((a, b) => {
    const valueA = selector(a);
    const valueB = selector(b);

    return order === "ascending" ? comparator(valueA, valueB) : comparator(valueB, valueA);
  });
}

function comparator<T>(a: T, b: T): number {
  if (b == null || b < a) {
    return -1;
  }
  if (b > a) {
    return 1;
  }
  return 0;
}
