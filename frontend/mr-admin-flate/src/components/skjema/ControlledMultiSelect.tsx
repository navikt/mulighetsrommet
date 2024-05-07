import React, { ReactNode } from "react";
import { Controller } from "react-hook-form";
import { MultiValue } from "react-select";
import { MultiSelect } from "./MultiSelect";
import { SelectOption } from "mulighetsrommet-frontend-common/components/SokeSelect";
import { HelpText } from "@navikt/ds-react";

export interface MultiSelectProps {
  label: string;
  placeholder: string;
  options: SelectOption[];
  readOnly?: boolean;
  size?: "small" | "medium";
  additionalOnChange?: (values: MultiValue<SelectOption<string>>) => void;
  name: string;
  helpText?: string;
  noOptionsMessage?: ReactNode;
}

export const ControlledMultiSelect = React.forwardRef(function ControlledMultiSelect(
  props: MultiSelectProps,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  _,
) {
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
              <MultiSelect
                size={size}
                error={Boolean(error)}
                placeholder={placeholder}
                noOptionsMessage={noOptionsMessage}
                childRef={ref}
                name={name}
                value={options.filter((c) => value?.includes(c.value))}
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
});
