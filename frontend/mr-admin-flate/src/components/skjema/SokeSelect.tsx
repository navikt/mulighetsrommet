import classnames from "classnames";
import React, { ForwardedRef } from "react";
import { Controller } from "react-hook-form";
import ReactSelect from "react-select";
import styles from "./SokeSelect.module.scss";
import { shallowEquals } from "../../utils/shallow-equals";

export interface SelectOption<T = string> {
  value: T;
  label: string;
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
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function SokeSelect<T>(props: SelectProps<T>, _: ForwardedRef<HTMLElement>) {
  const {
    label,
    hideLabel = false,
    placeholder,
    options,
    readOnly = false,
    onChange: providedOnChange,
    onInputChange: providedOnInputChange,
    description,
    className,
    size,
    onClearValue,
    ...rest
  } = props;

  return (
    <Controller
      {...rest}
      render={({ field: { onChange, value, name, ref }, fieldState: { error } }) => {
        const selectedOption = options.find((option) => shallowEquals(option.value, value));
        return (
          <div className={styles.container}>
            <label
              className={classnames(styles.label, {
                "navds-sr-only": hideLabel,
              })}
              style={{
                fontSize: size === "small" ? "16px" : "18px",
              }}
              htmlFor={name}
            >
              <b>{label}</b>
            </label>
            {description && (
              <label
                className={classnames(styles.description, {
                  "navds-sr-only": hideLabel,
                })}
                style={{
                  fontSize: size === "small" ? "16px" : "18px",
                }}
              >
                {description}
              </label>
            )}

            <ReactSelect
              placeholder={placeholder}
              isDisabled={readOnly}
              isClearable={!!onClearValue}
              ref={ref}
              inputId={name}
              noOptionsMessage={() => "Ingen funnet"}
              name={name}
              value={selectedOption ?? null}
              onChange={(e) => {
                onChange(e?.value);
                providedOnChange?.({
                  target: { value: e?.value, name: e?.label },
                });
                if (!e) {
                  onClearValue?.();
                }
              }}
              onInputChange={(e) => {
                providedOnInputChange?.(e);
              }}
              styles={customStyles(readOnly, Boolean(error))}
              options={options}
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
              <div className={styles.errormsg}>
                <b>• {error.message}</b>
              </div>
            )}
          </div>
        );
      }}
    />
  );
}

const customStyles = (readOnly: boolean, isError: boolean) => ({
  control: (provided: any, state: any) => ({
    ...provided,
    background: readOnly ? "#f1f1f1" : "#fff",
    borderColor: isError ? "#C30000" : readOnly ? "#0000001A" : "#0000008f",
    borderWidth: isError ? "2px" : "1px",
    boxShadow: state.isFocused ? null : null,
  }),
  clearIndicator: (provided: any) => ({
    ...provided,
    zIndex: "100",
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

const SokeSelectComponent = React.forwardRef(SokeSelect);

export { SokeSelectComponent as SokeSelect };
