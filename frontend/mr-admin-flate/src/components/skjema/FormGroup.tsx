import { PropsWithChildren } from "react";

export function FormGroup(props: PropsWithChildren) {
  return (
    <div className="flex flex-wrap flex-col gap-4 w-full mt-2 mb-2 bg-[var(--a-gray-50)] p-2 border border-[var(--a-border-divider)] rounded">
      {props.children}
    </div>
  );
}
