import { useState } from "react";
import { Button, DatePicker, VStack, useDatepicker } from "@navikt/ds-react";

interface DatePickerFormProps {
  label: string;
  description?: string;
  onSubmit: (data: { date: string }) => void;
  loading: boolean;
}

export function DatePickerForm({ label, description, onSubmit, loading }: DatePickerFormProps) {
  const [selectedDate, setSelectedDate] = useState<Date | undefined>(undefined);
  const [hasError, setHasError] = useState(false);

  const { datepickerProps, inputProps } = useDatepicker({
    onDateChange: setSelectedDate,
    defaultSelected: selectedDate,
    onValidate: (validation) => {
      setHasError(!validation.isValidDate);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedDate) {
      onSubmit({ date: selectedDate.toISOString().split("T")[0] });
    } else {
      setHasError(true);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <VStack gap="space-16" align="start">
        <DatePicker {...datepickerProps}>
          <DatePicker.Input
            {...inputProps}
            label={label}
            error={hasError && "Du må velge en gyldig dato"}
            description={description}
          />
        </DatePicker>
        <Button type="submit" loading={loading}>
          Run task 💥
        </Button>
      </VStack>
    </form>
  );
}
