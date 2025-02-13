import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Control, useForm } from "react-hook-form";
import { beforeEach, describe, expect, it } from "vitest";
import { ControlledDateInput } from "./ControlledDateInput";

interface Props {
  name: string;
  label: string;
  fromDate: Date;
  toDate: Date;
  format: "date" | "iso-string";
}

beforeEach(() => {
  cleanup();
});

// Test wrapper component to provide the form context
function TestWrapper({
  children,
  defaultValues = {},
}: {
  children: (control: Control) => React.ReactNode;
  defaultValues?: any;
}) {
  const { control } = useForm({ defaultValues });
  return children(control);
}

describe("ControlledDateInput", () => {
  const defaultProps: Props = {
    name: "testDate",
    label: "Test Date",
    fromDate: new Date("2024-01-01"),
    toDate: new Date("2024-12-31"),
    format: "date" as const,
  };

  it("renders with label", () => {
    render(
      <TestWrapper>
        {(control) => <ControlledDateInput {...defaultProps} control={control} />}
      </TestWrapper>,
    );

    expect(screen.getByLabelText("Test Date")).toBeDefined();
  });

  it("hides label when hideLabel is true", () => {
    render(
      <TestWrapper>
        {(control) => <ControlledDateInput {...defaultProps} control={control} hideLabel={true} />}
      </TestWrapper>,
    );

    const label = screen.getByText("Test Date");
    expect(label.classList.contains("navds-sr-only")).toBe(true);
  });

  it("shows placeholder text", () => {
    render(
      <TestWrapper>
        {(control) => (
          <ControlledDateInput {...defaultProps} control={control} placeholder="Enter date" />
        )}
      </TestWrapper>,
    );

    expect(screen.getByPlaceholderText("Enter date")).toBeDefined();
  });

  it("displays entered date value", async () => {
    const user = userEvent.setup();

    function TestForm() {
      const { control } = useForm({
        defaultValues: {
          testDate: null,
        },
      });

      return <ControlledDateInput {...defaultProps} name="testDate" control={control} />;
    }

    render(<TestForm />);

    const input = screen.getByLabelText("Test Date");
    await user.type(input, "01.01.2025");

    await waitFor(() => {
      expect((input as HTMLInputElement).value).toBe("01.01.2025");
    });
  });

  it("shows error message when date is invalid", async () => {
    const user = userEvent.setup();

    function TestForm() {
      const { control } = useForm({
        defaultValues: {
          fromDate: new Date("2024-01-01"),
          toDate: new Date("2024-12-31"),
          testDate: null,
        },
      });

      return (
        <ControlledDateInput
          {...defaultProps}
          name="testDate"
          control={control}
          invalidDatoEtterPeriode="Date must be within the valid period"
        />
      );
    }

    render(<TestForm />);

    const input = screen.getByLabelText("Test Date");
    await user.type(input, "01.01.2025");
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText("Date must be within the valid period")).toBeDefined();
    });
  });

  it("is disabled in readonly mode", () => {
    render(
      <TestWrapper>
        {(control) => <ControlledDateInput {...defaultProps} control={control} readOnly={true} />}
      </TestWrapper>,
    );

    expect(screen.getByLabelText("Test Date")).toHaveProperty("readOnly", true);
  });

  it("handles different size props correctly", () => {
    const { rerender } = render(
      <TestWrapper>
        {(control) => <ControlledDateInput {...defaultProps} control={control} size="small" />}
      </TestWrapper>,
    );

    expect(screen.getByLabelText("Test Date").classList.contains("navds-body-short--small")).toBe(
      true,
    );

    rerender(
      <TestWrapper>
        {(control) => <ControlledDateInput {...defaultProps} control={control} size="medium" />}
      </TestWrapper>,
    );

    expect(screen.getByLabelText("Test Date").classList.contains("navds-body-short--medium")).toBe(
      true,
    );
  });
});
