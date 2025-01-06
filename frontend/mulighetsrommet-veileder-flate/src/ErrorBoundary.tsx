import { PropsWithChildren } from "react";
import { PORTEN_URL } from "@/constants";
import { InlineErrorBoundary as CommonInlineErrorBoundary } from "@mr/frontend-common";

export function ArbeidsmarkedstiltakErrorBoundary(props: PropsWithChildren) {
  return (
    <CommonInlineErrorBoundary portenUrl={PORTEN_URL}>{props.children}</CommonInlineErrorBoundary>
  );
}
