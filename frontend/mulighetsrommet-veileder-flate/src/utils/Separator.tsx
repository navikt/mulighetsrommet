export const Separator = ({ providedStyle }: { providedStyle?: any }) => (
  <hr
    style={{
      backgroundColor: "var(--a-border-divider)",
      height: "1px",
      border: "none",
      width: "100%",
      margin: "1.5rem 0",
      ...providedStyle,
    }}
  />
);
