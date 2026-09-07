interface Props {
  value: string | null | undefined;
}

export function JsonOrTextValue({ value }: Props) {
  if (!value) {
    return <pre>{value}</pre>;
  }

  const asJson = tryParseJson(value);
  return <pre>{asJson ?? value}</pre>;
}

function tryParseJson(value: string): string | null {
  try {
    const parsed = JSON.parse(value);
    // Only objects and arrays benefit from pretty-printing; plain strings/numbers/booleans
    // parse "successfully" as JSON but aren't meaningfully different from the raw value.
    if (typeof parsed !== "object" || parsed === null) {
      return null;
    }
    return JSON.stringify(parsed, null, 2);
  } catch {
    return null;
  }
}
