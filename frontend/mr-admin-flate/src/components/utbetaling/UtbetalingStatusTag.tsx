import { UtbetalingStatusDto } from "@tiltaksadministrasjon/api-client";
import { ReactNode } from "react";
import { DataElementStatusTag } from "@/components/data-element/DataElementStatusTag";

export function UtbetalingStatusTag({ status }: { status: UtbetalingStatusDto }): ReactNode {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
