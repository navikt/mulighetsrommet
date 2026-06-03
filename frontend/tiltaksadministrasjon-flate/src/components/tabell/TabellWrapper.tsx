interface Props {
  children: React.ReactNode;
  className?: string;
}

export function TabellWrapper({ children, className }: Props) {
  return <div className={`bg-ax-bg-default mb-8 ${className ?? ""}`}>{children}</div>;
}
