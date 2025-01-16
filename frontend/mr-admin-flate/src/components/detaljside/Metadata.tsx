import { ReactNode } from "react";
import classNames from "classnames";

export interface MetadataProps {
  header: string | ReactNode;
  verdi: string | number | undefined | null | ReactNode;
}

export function Metadata({ header, verdi }: MetadataProps) {
  return (
    <div className="flex flex-col gap-2">
      <dt className="font-bold">{header}</dt>
      <dd className="mr-6 whitespace-pre-line">{verdi ?? "-"}</dd>
    </div>
  );
}

export function Separator({ style, classname }: { style?: any; classname?: string }) {
  return (
    <hr
      style={style}
      className={classNames("bg-[var(--a-border-divider)] h-px border-0 w-full my-4", classname)}
    />
  );
}
