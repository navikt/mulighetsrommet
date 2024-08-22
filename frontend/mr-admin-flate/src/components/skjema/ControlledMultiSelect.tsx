import React, { ForwardedRef, ReactNode } from "react";
import { Controller } from "react-hook-form";
import { MultiValue } from "react-select";
import { MultiSelect } from "./MultiSelect";
import { SelectOption } from "@mr/frontend-common/components/SokeSelect";
import { HelpText } from "@navikt/ds-react";
import { shallowEquals } from "@mr/frontend-common";

export interface MultiSelectProps<T> {
  label: string;
  placeholder: string;
  options: SelectOption<T>[];
  readOnly?: boolean;
  size?: "small" | "medium";
  additionalOnChange?: (values: MultiValue<SelectOption<T>>) => void;
  onInputChange?: (value: string) => void;
  name: string;
  helpText?: string;
  noOptionsMessage?: ReactNode;
  velgAlle?: boolean;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function ControlledMultiSelect<T>(props: MultiSelectProps<T>, _: ForwardedRef<HTMLElement>) {
  const {
    name,
    size,
    noOptionsMessage,
    helpText,
    label,
    placeholder,
    options,
    readOnly,
    additionalOnChange,
    onInputChange,
    velgAlle = false,
    ...rest
  } = props;

  return (
    <div>
      <Controller
        name={name}
        {...rest}
        render={({ field: { onChange, value, name, ref }, fieldState: { error } }) => {
          return (
            <>
              <div
                style={{
                  display: "flex",
                  gap: "0.25rem",
                  flexDirection: "row",
                }}
              >
                <label
                  style={{
                    fontSize: size === "small" ? "16px" : "18px",
                    marginBottom: "8px",
                    display: "inline-block",
                  }}
                  htmlFor={name}
                >
                  <b>{label}</b>
                </label>
                {helpText && <HelpText>{helpText}</HelpText>}
              </div>
              <MultiSelect<T>
                size={size}
                velgAlle={velgAlle}
                error={Boolean(error)}
                placeholder={placeholder}
                noOptionsMessage={noOptionsMessage}
                onInputChange={onInputChange}
                childRef={ref}
                name={name}
                value={options.filter((c: SelectOption<T>) =>
                  value?.some((v: T) => shallowEquals(v, c.value)),
                )}
                onChange={(e) => {
                  onChange(e?.map((option: SelectOption) => option.value));
                  additionalOnChange?.(e);
                }}
                options={options}
                readOnly={readOnly}
              />
              {error && (
                <div
                  style={{
                    marginTop: "8px",
                    color: "#C30000",
                    fontSize: size === "small" ? "16px" : "18px",
                  }}
                >
                  <b>• {error.message}</b>
                </div>
              )}
            </>
          );
        }}
      />
    </div>
  );
}

const Component = React.forwardRef(ControlledMultiSelect);

export { Component as ControlledMultiSelect };
