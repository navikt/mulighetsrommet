import { UseMutationResult } from "@tanstack/react-query";
import debounce from "debounce";
import { Utkast } from "mulighetsrommet-api-client";
import { memo, useCallback, useEffect } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { toast } from "react-toastify";
import useDeepCompareEffect from "use-deep-compare-effect";
import { inferredTiltaksgjennomforingSchema } from "../tiltaksgjennomforinger/TiltaksgjennomforingSchema";
import { AutoSaveToastContainer } from "./AutoSaveToastContainer";

type Props = {
  defaultValues: any;
  utkastId: string;
  onSave: () => void;
  mutation: UseMutationResult<Utkast, unknown, Utkast, unknown>;
};

export const AutoSaveUtkast = memo(
  ({ defaultValues, utkastId, onSave, mutation }: Props) => {
    if (!utkastId) throw new Error("Ingen utkastId tilgjengelig");

    const methods = useFormContext<inferredTiltaksgjennomforingSchema>();

    const debouncedSave = useCallback(
      debounce(() => {
        onSave();
      }, 1000),
      [],
    );

    useEffect(() => {
      if (mutation.isSuccess) {
        toast.success("Utkast lagret", {
          toastId: `success-${utkastId}`, // For å hindre duplikate meldinger
          hideProgressBar: true,
          autoClose: 2000,
        });
      }

      if (mutation.isError) {
        toast.error("Klarte ikke lagre utkast", {
          toastId: `error-${utkastId}`, // For å hindre duplikate meldinger
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

    return <AutoSaveToastContainer />;
  },
);

AutoSaveUtkast.displayName = "AutoSaveUtkast";
