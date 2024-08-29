import { HelpText } from "@navikt/ds-react";
import classnames from "classnames";
import { Ref } from "react";
import ReactSelect from "react-select";
import Select from "react-select/base";

export interface SelectOption<T = string> {
  value: T;
  label: string;
  isDisabled?: boolean;
}

export interface SelectProps<T> {
  label: string;
  hideLabel?: boolean;
  placeholder: string;
  options: SelectOption<T>[];
  readOnly?: boolean;
  name: string;
  onChange?: (a0: {
    target: {
      value?: T;
      name?: string;
    };
  }) => void;
  onInputChange?: (input: string) => void;
  className?: string;
  size?: "small" | "medium";
  onClearValue?: () => void;
  description?: string;
  value: SelectOption<T> | null;
  error?: { message?: string };
  helpText?: React.ReactNode;
}

export function SokeSelect<T>(props: SelectProps<T> & { childRef?: Ref<Select<SelectOption<T>>> }) {
  const {
    label,
    hideLabel = false,
    placeholder,
    options,
    readOnly = false,
    onChange,
    onInputChange,
    description,
    className,
    size,
    onClearValue,
    error,
    name,
    value,
    helpText,
    childRef,
  } = props;

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
      }}
    >
      <div
        style={{
          display: "flex",
          alignItems: "start",
          gap: "0.5rem",
          flexDirection: "row",
        }}
      >
        <label
          className={classnames({
            "navds-sr-only": hideLabel,
          })}
          style={{
            fontSize: size === "small" ? "16px" : "18px",
            display: "inline-block",
            marginBottom: "8px",
          }}
          htmlFor={name}
        >
          <b>{label}</b>
        </label>
        {helpText && <HelpText>{helpText}</HelpText>}
      </div>
      {description && (
        <label
          className={classnames({
            "navds-sr-only": hideLabel,
          })}
          style={{
            fontSize: size === "small" ? "16px" : "18px",
            marginBottom: "8px",
            display: "inline-block",
            color: "var(--ac-form-description, var(--a-text-subtle))",
          }}
        >
          {description}
        </label>
      )}

      <ReactSelect
        placeholder={placeholder}
        isDisabled={readOnly}
        isClearable={!!onClearValue}
        ref={childRef}
        inputId={name}
        noOptionsMessage={() => "Ingen funnet"}
        name={name}
        value={value}
        onChange={(e) => {
          onChange?.({
            target: { value: e?.value, name: e?.label },
          });
          if (!e) {
            onClearValue?.();
          }
        }}
        onInputChange={(e) => {
          onInputChange?.(e);
        }}
        styles={customStyles(readOnly, Boolean(error))}
        options={options}
        isOptionDisabled={(option) => !!option.isDisabled}
        className={className}
        theme={(theme) => ({
          ...theme,
          spacing: {
            ...theme.spacing,
            controlHeight: size === "small" ? 32 : 48,
            baseUnit: 2,
          },
          colors: {
            ...theme.colors,
            primary25: "#cce1ff",
            primary: "#0067c5",
          },
        })}
      />
      {error && (
        <div
          style={{
            marginTop: "8px",
            color: "#c30000",
            fontSize: size === "small" ? "16px" : "18px",
          }}
        >
          <b>• {error.message}</b>
        </div>
      )}
    </div>
  );
}

const customStyles = (readOnly: boolean, isError: boolean) => ({
  control: (provided: any, state: any) => {
    return {
      ...provided,
      background: readOnly ? "#f1f1f1" : "#fff",
      borderColor: isError ? "#C30000" : readOnly ? "#0000001A" : "#0000008f",
      borderWidth: isError ? "2px" : "1px",
      boxShadow: state.isFocused ? "0 0 0 3px rgba(0, 52, 125, 1)" : null,
    };
  },
  clearIndicator: (provided: any) => ({
    ...provided,
  }),
  indicatorSeparator: () => ({
    display: "none",
  }),
  dropdownIndicator: (provided: any) => ({
    ...provided,
    color: "black",
  }),
  placeholder: (provided: any) => ({
    ...provided,
    color: "#0000008f",
  }),
  singleValue: (provided: any) => ({
    ...provided,
    color: "black",
  }),
  menu: (provided: any) => ({
    ...provided,
    zIndex: "1000",
  }),
});
