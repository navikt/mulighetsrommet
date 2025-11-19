export type DataDrivenTableDto = {
  columns: Array<DataDrivenTableDtoColumn>;
  rows: Array<DataDrivenTableDtoRow>;
};

export type DataDrivenTableDtoColumn = {
  key: string;
  label: null | string;
  sortable: boolean;
  align: DataDrivenTableDtoColumnAlign;
};

export enum DataDrivenTableDtoColumnAlign {
  LEFT = "left",
  CENTER = "center",
  RIGHT = "right",
}

export type DataDrivenTableDtoRow = {
  cells: {
    [key: string]: DataElement;
  };
  content: null | TimelineDto;
};

export type DataElement =
  | ({
      type?: "no.nav.mulighetsrommet.model.DataElement.Link";
    } & DataElementLink)
  | ({
      type?: "no.nav.mulighetsrommet.model.DataElement.MathOperator";
    } & DataElementMathOperator)
  | ({
      type?: "no.nav.mulighetsrommet.model.DataElement.MultiLinkModal";
    } & DataElementMultiLinkModal)
  | ({
      type?: "no.nav.mulighetsrommet.model.DataElement.Periode";
    } & DataElementPeriode)
  | ({
      type?: "no.nav.mulighetsrommet.model.DataElement.Status";
    } & DataElementStatus)
  | ({
      type?: "no.nav.mulighetsrommet.model.DataElement.Text";
    } & DataElementText);

export type DataElementLink = {
  text: string;
  href: string;
  digest: string;
};

export type DataElementMathOperator = {
  operator: DataElementMathOperatorType;
};

export enum DataElementMathOperatorType {
  PLUS = "plus",
  MINUS = "minus",
  MULTIPLY = "multiply",
  DIVIDE = "divide",
  EQUALS = "equals",
}

export type DataElementMultiLinkModal = {
  buttonText: string;
  modalContent: DataElementMultiLinkModalModalContent;
};

export type DataElementMultiLinkModalModalContent = {
  header: string;
  description: string;
  links: Array<DataElementLink>;
};

export type DataElementPeriode = {
  start: string;
  slutt: string;
};

export type DataElementStatus = {
  value: string;
  variant: DataElementStatusVariant;
  description: null | string;
};

export enum DataElementStatusVariant {
  BLANK = "blank",
  NEUTRAL = "neutral",
  ALT = "alt",
  ALT_1 = "alt-1",
  ALT_2 = "alt-2",
  ALT_3 = "alt-3",
  INFO = "info",
  SUCCESS = "success",
  WARNING = "warning",
  ERROR = "error",
  ERROR_BORDER = "error-border",
  ERROR_BORDER_STRIKETHROUGH = "error-border-strikethrough",
}

export type DataElementText = {
  value: null | string;
  format: null | DataElementTextFormat;
};

export enum DataElementTextFormat {
  DATE = "date",
  NOK = "nok",
  NUMBER = "number",
}

export type TimelineDto = {
  startDate: string;
  endDate: string;
  rows: Array<TimelineDtoRow>;
};

export type TimelineDtoRow = {
  periods: Array<TimelineDtoRowPeriod>;
  label: string;
};

export type TimelineDtoRowPeriod = {
  key: string;
  start: string;
  end: string;
  status: TimelineDtoRowPeriodVariant;
  content: string;
  hover: string;
};

export enum TimelineDtoRowPeriodVariant {
  INFO = "info",
  SUCCESS = "success",
  WARNING = "warning",
  DANGER = "danger",
  NEUTRAL = "neutral",
}

export type LabeledDataElement = {
  type: LabeledDataElementType;
  label: string;
  value: null | DataElement;
};

export enum LabeledDataElementType {
  INLINE = "INLINE",
  MULTILINE = "MULTILINE",
}
