import { memo, useCallback, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import debounce from "debounce";
import useDeepCompareEffect from "use-deep-compare-effect";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { Utkast } from "mulighetsrommet-api-client";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { formaterDatoTid } from "../../utils/Utils";

type Props = {
  defaultValues: any;
  utkastId: string;
};

export const AutoSaveTiltaksgjennomforing = memo(
  ({ defaultValues, utkastId }: Props) => {
    if (!utkastId) throw new Error("Ingen utkastId tilgjengelig");
    const methods = useFormContext();
    const { data } = useHentAnsatt();
    const mutation = useMutateUtkast();
    const [savedTs, setSavedTs] = useState<Date | null>(null);
    const debouncedSave = useCallback(
      debounce(() => {
        console.log("Saving");
        const utkastData = methods.getValues();
        mutation.mutate({
          id: utkastId,
          utkastData,
          type: Utkast.type.TILTAKSGJENNOMFORING,
          opprettetAv: data?.navIdent,
        });
        setSavedTs(new Date());
      }, 1000),
      []
    );

    const watchedData = useWatch({
      control: methods.control,
      defaultValue: defaultValues,
    });

    useDeepCompareEffect(() => {
      if (methods.formState.isDirty) {
        debouncedSave();
      }
    }, [watchedData]);

    return savedTs ? (
      <span>Lagret utkast: {formaterDatoTid(savedTs)}</span>
    ) : null;
  }
);

AutoSaveTiltaksgjennomforing.displayName = "AutoSaveTiltaksgjennomforing";
