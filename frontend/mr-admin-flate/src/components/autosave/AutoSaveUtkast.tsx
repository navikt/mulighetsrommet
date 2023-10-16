import { UseMutationResult } from "@tanstack/react-query";
import debounce from "debounce";
import { Utkast } from "mulighetsrommet-api-client";
import { memo, useCallback, useEffect, useState } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { toast } from "react-toastify";
import useDeepCompareEffect from "use-deep-compare-effect";
import { PencilWritingIcon } from "@navikt/aksel-icons";
import { BodyShort } from "@navikt/ds-react";
import styles from "./AutoSaveUtkast.module.scss";
import { formaterDatoTid } from "../../utils/Utils";
import { InferredTiltaksgjennomforingSchema } from "../tiltaksgjennomforinger/TiltaksgjennomforingSchema";

type Props = {
  defaultValues: any;
  utkastId: string;
  defaultUpdatedAt?: string;
  onSave: () => void;
  mutationUtkast: UseMutationResult<Utkast, unknown, Utkast>;
};

export const AutoSaveUtkast = memo(
  ({ defaultValues, utkastId, onSave, mutationUtkast, defaultUpdatedAt }: Props) => {
    if (!utkastId) throw new Error("Ingen utkastId tilgjengelig");

    const [lagreState, setLagreState] = useState(
      defaultUpdatedAt ? formaterDatoTid(defaultUpdatedAt) : undefined,
    );
    const methods = useFormContext<InferredTiltaksgjennomforingSchema>();

    const debouncedSave = useCallback(
      debounce(() => {
        onSave();
      }, 1000),
      [],
    );

    useEffect(() => {
      if (mutationUtkast.isLoading) {
        setLagreState("Lagrer...");
      }

      if (mutationUtkast.isSuccess) {
        setLagreState(formaterDatoTid(mutationUtkast.data?.updatedAt));
      }

      if (mutationUtkast.isError) {
        toast.error("Klarte ikke lagre utkast", {
          toastId: `error-${utkastId}`, // For å hindre duplikate meldinger
          hideProgressBar: true,
        });
      }
    }, [mutationUtkast]);

    const watchedData = useWatch({
      control: methods.control,
      defaultValue: defaultValues,
    });

    useDeepCompareEffect(() => {
      if (methods.formState.isDirty) {
        debouncedSave();
      }
    }, [watchedData]);

    return (
      <>
        {lagreState ? (
          <div
            className={styles.autosave}
            // title={`Siste lagrede utkast: ${mutationUtkast.data!.updatedAt}`}
          >
            <PencilWritingIcon />
            <BodyShort>Sist lagret: {lagreState}</BodyShort>
          </div>
        ) : null}
      </>
    );
  },
);

AutoSaveUtkast.displayName = "AutoSaveUtkast";
