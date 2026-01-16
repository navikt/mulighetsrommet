import { ReactNode } from "react";
import { DataElementCurrencyValue as DataElementCurrencyValueType } from "./types";
import { formaterValuta } from "../../utils/utils";

interface DataElementCurrencyProps {
  data: DataElementCurrencyValueType;
}

export function DataElementCurrencyValue({ data }: DataElementCurrencyProps): ReactNode {
  const num = !data.value ? NaN : parseInt(data.value);
  if (isNaN(num)) {
    return "";
  }
  return formaterValuta(num, data.currency);
}
