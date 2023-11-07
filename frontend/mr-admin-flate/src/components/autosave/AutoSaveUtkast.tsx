import { PencilWritingIcon } from "@navikt/aksel-icons";
import { BodyShort } from "@navikt/ds-react";
import { UseMutationResult } from "@tanstack/react-query";
import debounce from "debounce";
import { UtkastDto as Utkast, UtkastRequest } from "mulighetsrommet-api-client";
import { memo, useCallback, useEffect } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import useDeepCompareEffect from "use-deep-compare-effect";
import { formaterDatoTid } from "../../utils/Utils";
import styles from "./AutoSaveUtkast.module.scss";

type Props = {
  defaultValues: any;
  utkastId: string;
  onSave: () => void;
  mutationUtkast: UseMutationResult<Utkast, unknown, UtkastRequest>;
  lagreState?: string;
  setLagreState: (state: string) => void;
};

export const AutoSaveUtkast = memo(
  ({ defaultValues, utkastId, onSave, mutationUtkast, lagreState, setLagreState }: Props) => {
    if (!utkastId) throw new Error("Ingen utkastId tilgjengelig");

    const methods = useFormContext();

    const debouncedSave = useCallback(
      debounce(() => {
        onSave();
      }, 1000),
      [],
    );

    useEffect(() => {
      if (mutationUtkast.isPending) {
        setLagreState("Lagrer utkast...");
      }

      if (mutationUtkast.isSuccess) {
        setLagreState(`Sist lagret utkast: ${formaterDatoTid(mutationUtkast.data?.updatedAt)}`);
      }

      if (mutationUtkast.isError) {
        setLagreState("Klarte ikke å lagre utkast");
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
          <div className={styles.autosave}>
            <PencilWritingIcon />
            <BodyShort>{lagreState}</BodyShort>
          </div>
        ) : null}
      </>
    );
  },
);

AutoSaveUtkast.displayName = "AutoSaveUtkast";
