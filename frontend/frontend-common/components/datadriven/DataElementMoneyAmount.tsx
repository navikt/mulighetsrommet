import { ReactNode } from "react";
import { DataElementMoneyAmount as DataElementMoneyAmountType } from "./types";
import { formaterValuta } from "../../utils/utils";

interface Props {
  data: DataElementMoneyAmountType;
}

export function DataElementMoneyAmount({ data }: Props): ReactNode {
  const num = !data.value ? NaN : parseInt(data.value);
  if (isNaN(num)) {
    return "";
  }
  return formaterValuta(num, data.currency);
}
