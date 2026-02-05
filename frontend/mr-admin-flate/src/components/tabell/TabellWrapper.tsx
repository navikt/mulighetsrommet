interface Props {
  filterOpen?: boolean;
  children: React.ReactNode;
  className?: string;
}

export function TabellWrapper({ filterOpen = false, children, className }: Props) {
  return (
    <div
      className={`bg-ax-bg-default mb-8 ${filterOpen ? "ml-2" : "ml-0"} ax-xl:ml-0 ${className ?? ""}`}
    >
      {children}
    </div>
  );
}
