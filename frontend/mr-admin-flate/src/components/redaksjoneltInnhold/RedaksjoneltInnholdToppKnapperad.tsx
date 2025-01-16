import { PropsWithChildren } from "react";

export function RedaksjoneltInnholdToppKnapperad(props: PropsWithChildren) {
  return (
    <div className="max-w-[900px] mt-4">
      {props.children}
      <hr className="w-full h-px my-4 bg-border-divider border-0" />
    </div>
  );
}
