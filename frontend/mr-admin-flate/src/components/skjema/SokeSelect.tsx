import classnames from "classnames";
import React from "react";
import { Controller } from "react-hook-form";
import ReactSelect from "react-select";
import styles from "./SokeSelect.module.scss";

export interface SelectOption {
  value?: string;
  label?: string;
}

export interface SelectProps {
  label: string;
  hideLabel?: boolean;
  placeholder: string;
  options: SelectOption[];
  readOnly?: boolean;
  onChange?: (a0: any) => void;
  onInputChange?: (a0: any) => void;
  className?: string;
  size?: "small" | "medium";
  onClearValue?: () => void;
  description?: string;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const SokeSelect = React.forwardRef((props: SelectProps, _) => {
  const {
    label,
    hideLabel = false,
    placeholder,
    options,
    readOnly,
    onChange: providedOnChange,
    onInputChange: providedOnInputChange,
    description,
    className,
    size,
    onClearValue,
    ...rest
  } = props;

  const customStyles = (isError: boolean) => ({
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

  return (
    <>
      <Controller
        name={label}
        {...rest}
        render={({
          field: { onChange, value, name, ref },
          fieldState: { error },
        }) => {
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
              { description &&
                <label
                  className={classnames(styles.description, {
                    "navds-sr-only": hideLabel,
                  })}
                  style={{
                    fontSize: size === "small" ? "16px" : "18px",
                  }}
                >
                  { description }
                </label>
              }

              <ReactSelect
                key={`${value}`} // Force rerender when value changes. If set to null outside f. ex
                placeholder={placeholder}
                isDisabled={!!readOnly}
                isClearable={!!onClearValue}
                ref={ref}
                inputId={name}
                noOptionsMessage={() => "Ingen funnet"}
                name={name}
                value={options.find((c) => c.value === value)}
                onChange={(e) => {
                  onChange(e?.value);
                  providedOnChange?.(e?.value);
                  if (!e) {
                    onClearValue?.();
                  }
                }}
                onInputChange={(e) => {
                  providedOnInputChange?.(e);
                }}
                styles={customStyles(Boolean(error))}
                options={options}
                className={className}
                theme={(theme: any) => ({
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
    </>
  );
});

SokeSelect.displayName = "SokeSelect";

export { SokeSelect };
