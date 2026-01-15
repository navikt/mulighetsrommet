import { ReactNode } from "react";
import {
  DataElementCurrencyValueCurrency,
  DataElementCurrencyValue as DataElementCurrencyValueType,
} from "./types";
import { formaterNOK } from "../../utils/utils";

interface DataElementCurrencyProps {
  data: DataElementCurrencyValueType;
}

export function DataElementCurrencyValue({ data }: DataElementCurrencyProps): ReactNode {
  const num = !data.value ? NaN : parseInt(data.value);
  if (!num) {
    return "";
  }
  switch (data.currency) {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-condition
    case DataElementCurrencyValueCurrency.NOK:
      return formaterNOK(num);
  }
}
