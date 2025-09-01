import { DataElementMathOperatorType } from "@mr/api-client-v2";
import { ReactNode } from "react";

interface DataElementMathOperatorProps {
  operator: DataElementMathOperatorType;
}

export function DataElementMathOperator({ operator }: DataElementMathOperatorProps): ReactNode {
  switch (operator) {
    case DataElementMathOperatorType.PLUS:
      return "+";
    case DataElementMathOperatorType.MINUS:
      return "−";
    case DataElementMathOperatorType.MULTIPLY:
      return "×";
    case DataElementMathOperatorType.DIVIDE:
      return "÷";
    case DataElementMathOperatorType.EQUALS:
      return "=";
  }
}
