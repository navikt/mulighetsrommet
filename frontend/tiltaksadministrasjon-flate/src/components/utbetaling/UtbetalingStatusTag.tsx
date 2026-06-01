import { DataElementStatusTag } from "@mr/frontend-common";
import { UtbetalingStatusDto } from "@tiltaksadministrasjon/api-client";
import { ReactNode } from "react";

export function UtbetalingStatusTag({ status }: { status: UtbetalingStatusDto }): ReactNode {
  return <DataElementStatusTag value={status.status.value} variant={status.status.variant} />;
}
