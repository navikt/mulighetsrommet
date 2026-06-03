export function Separator({ providedStyle }: { providedStyle?: any }) {
  return (
    <hr
      style={{
        backgroundColor: "var(--ax-border-neutral-subtle)",
        height: "1px",
        border: "none",
        width: "100%",
        margin: "1.5rem 0",
        ...providedStyle,
      }}
    />
  );
}
