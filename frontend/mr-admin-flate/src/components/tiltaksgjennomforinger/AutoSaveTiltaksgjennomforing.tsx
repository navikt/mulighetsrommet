import debounce from "debounce";
import { Utkast } from "mulighetsrommet-api-client";
import { memo, useCallback, useEffect } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { toast } from "react-toastify";
import useDeepCompareEffect from "use-deep-compare-effect";
import { useHentAnsatt } from "../../api/ansatt/useHentAnsatt";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";

type Props = {
  defaultValues: any;
  utkastId: string;
};
// TODO Vurdere å ta ting inn som props og gjenbruke autosave på tvers av gjennomføring og avtale
export const AutoSaveTiltaksgjennomforing = memo(
  ({ defaultValues, utkastId }: Props) => {
    if (!utkastId) throw new Error("Ingen utkastId tilgjengelig");
    const methods = useFormContext();
    const { data } = useHentAnsatt();
    const mutation = useMutateUtkast();
    const debouncedSave = useCallback(
      debounce(() => {
        const utkastData = methods.getValues();
        mutation.mutate({
          id: utkastId,
          utkastData,
          type: Utkast.type.TILTAKSGJENNOMFORING,
          opprettetAv: data?.navIdent, // Bør bare settes ved første gang lagring av utkast
        });
      }, 1000),
      []
    );

    useEffect(() => {
      if (mutation.isSuccess) {
        toast.success("Lagret utkast", {
          toastId: `success-${utkastId}`, // For å hindre duplikate meldinger
          hideProgressBar: true,
          autoClose: 2000,
        });
      }

      if (mutation.isError) {
        toast.error("Klarte ikke lagre utkast", {
          toastId: `error-${utkastId}`,
          hideProgressBar: true,
        });
      }
    }, [mutation]);

    const watchedData = useWatch({
      control: methods.control,
      defaultValue: defaultValues,
    });

    useDeepCompareEffect(() => {
      if (methods.formState.isDirty) {
        debouncedSave();
      }
    }, [watchedData]);

    return null;
  }
);

AutoSaveTiltaksgjennomforing.displayName = "AutoSaveTiltaksgjennomforing";
