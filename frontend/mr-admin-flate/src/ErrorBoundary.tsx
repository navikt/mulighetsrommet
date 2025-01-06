import { PropsWithChildren } from "react";
import { PORTEN_URL } from "@/constants";
import {
  InlineErrorBoundary as CommonInlineErrorBoundary,
  ReloadAppErrorBoundary as CommonReloadAppErrorBoundary,
} from "@mr/frontend-common";

export function InlineErrorBoundary(props: PropsWithChildren) {
  return (
    <CommonInlineErrorBoundary portenUrl={PORTEN_URL}>{props.children}</CommonInlineErrorBoundary>
  );
}

export function ReloadAppErrorBoundary(props: PropsWithChildren) {
  return (
    <CommonReloadAppErrorBoundary portenUrl={PORTEN_URL}>
      {props.children}
    </CommonReloadAppErrorBoundary>
  );
}
