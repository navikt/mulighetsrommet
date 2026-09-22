export type AlertVariant = "info" | "warning" | "error";

const ALERT_VARIANTS: AlertVariant[] = ["info", "warning", "error"];

// Sanity lagrer `variant` som en liste (f.eks. ["warning"]), mens Aksel sin
// Alert forventer en enkelt streng. Uten denne normaliseringen sendes en array
// inn i Alert, som internt kaller variant.split(...) og kaster
// "TypeError: e.split is not a function".
export function utledAlertVariant(variant: AlertVariant | AlertVariant[]): AlertVariant {
  const value = Array.isArray(variant) ? variant[0] : variant;
  return ALERT_VARIANTS.includes(value) ? value : "info";
}
