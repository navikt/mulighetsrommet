import { PropsWithChildren } from "react";

export function FormGroup(props: PropsWithChildren) {
  return (
    <div className="flex flex-wrap flex-col gap-4 w-full mt-2 mb-2 bg-[var(--ax-neutral-100)] p-2 border border-[var(--ax-border-neutral-subtle)] rounded">
      {props.children}
    </div>
  );
}
