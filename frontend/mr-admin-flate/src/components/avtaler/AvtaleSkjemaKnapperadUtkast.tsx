import styles from "../skjema/Skjema.module.scss";
import {
  Avtale,
  AvtaleRequest,
  Toggles,
  Utkast,
} from "mulighetsrommet-api-client";
import AvbrytAvtaleGjennomforingModal from "./AvbrytAvtaleGjennomforingModal";
import { useState } from "react";
import { SlettUtkastKnapp } from "../knapper/SlettUtkastKnapp";
import SletteModal from "../modal/SletteModal";
import { OpprettAvtaleGjennomforingKnapp } from "../knapper/OpprettAvtaleGjennomforingKnapp";
import { UseMutationResult } from "@tanstack/react-query";
import { LagreEndringerKnapp } from "../knapper/LagreEndringerKnapp";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { useMineUtkast } from "../../api/utkast/useMineUtkast";
import { useMutateUtkast } from "../../api/utkast/useMutateUtkast";
import { useDeleteUtkast } from "../../api/utkast/useDeleteUtkast";
import { useAvbrytAvtale } from "../../api/avtaler/useAvbrytAvtale";

interface Props {
  handleDelete: () => void;
  avtale: Avtale;
  utkast: Utkast;
  utkastModus: boolean;
  mutation: UseMutationResult<Avtale, unknown, AvtaleRequest>;
  onLagreUtkast: () => void;
}
export function AvtaleSkjemaKnapperadUtkast({
  handleDelete,
  avtale,
  utkast,
  utkastModus,
  mutation,
  onLagreUtkast,
}: Props) {
  const { data: slettAvtaleEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_SLETT_AVTALE,
  );
  const [avbrytModalOpen, setAvbrytModalOpen] = useState(false);
  const { refetch } = useMineUtkast(utkast.type);
  const [utkastIdForSletting, setUtkastIdForSletting] = useState<null | string>(
    null,
  );
  const mutationDeleteUtkast = useDeleteUtkast();
  const mutationUtkast = useMutateUtkast();
  const mutationAvbryt = useAvbrytAvtale();

  async function onDelete() {
    mutationDeleteUtkast.mutate(utkast.id, {
      onSuccess: async () => {
        handleDelete();
        setUtkastIdForSletting(null);
        await refetch();
      },
    });
  }

  return utkastModus ? (
    <>
      <div className={styles.button_row}>
        {slettAvtaleEnabled ? (
          <SlettUtkastKnapp
            setSletteModal={() => setUtkastIdForSletting(utkast.id)}
          />
        ) : null}
        <div>
          <LagreEndringerKnapp submit={false} onLagreUtkast={onLagreUtkast} />
          <OpprettAvtaleGjennomforingKnapp type="avtale" mutation={mutation} />
        </div>
      </div>

      <AvbrytAvtaleGjennomforingModal
        modalOpen={avbrytModalOpen}
        onClose={() => {
          setAvbrytModalOpen(false);
        }}
        data={avtale}
        mutationAvbryt={mutationAvbryt}
        type="avtale"
      />
      <SletteModal
        modalOpen={!!utkastIdForSletting}
        onClose={() => setUtkastIdForSletting(null)}
        headerText="Ønsker du å slette utkastet?"
        headerTextError="Kan ikke slette utkastet."
        handleDelete={onDelete}
        mutation={mutationUtkast}
      />
    </>
  ) : null;
}
