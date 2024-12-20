import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Header } from "@/components/detaljside/Header";
import headerStyles from "@/components/detaljside/Header.module.scss";
import { TiltaksgjennomforingIkon } from "@/components/ikoner/TiltaksgjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { DupliserTiltak } from "@/components/tiltaksgjennomforinger/DupliserTiltak";
import { PREVIEW_ARBEIDSMARKEDSTILTAK_URL } from "@/constants";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { Toggles } from "@mr/api-client";
import { GjennomforingStatusMedAarsakTag } from "@mr/frontend-common";
import { Lenkeknapp } from "@mr/frontend-common/components/lenkeknapp/Lenkeknapp";
import { gjennomforingIsAktiv } from "@mr/frontend-common/utils/utils";
import { Alert, Heading, Tabs, VStack } from "@navikt/ds-react";
import classNames from "classnames";
import { Link, Outlet, useLoaderData, useLocation, useParams } from "react-router";
import commonStyles from "../Page.module.scss";
import { tiltaksgjennomforingLoader } from "./tiltaksgjennomforingLoaders";

function createBrodsmuler(
  tiltaksgjennomforingId: string,
  avtaleId?: string,
  tilsagn?: boolean,
  refusjonskrav?: boolean,
): Array<Brodsmule | undefined> {
  return [
    { tittel: "Forside", lenke: "/" },
    avtaleId
      ? { tittel: "Avtaler", lenke: "/avtaler" }
      : { tittel: "Tiltaksgjennomføringer", lenke: "/tiltaksgjennomforinger" },
    avtaleId ? { tittel: "Avtaledetaljer", lenke: `/avtaler/${avtaleId}` } : undefined,
    avtaleId
      ? {
          tittel: "Avtalens gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
    {
      tittel: "Tiltaksgjennomføringdetaljer",
      lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}`,
    },
    tilsagn
      ? { tittel: "Tilsagn", lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}/tilsagn` }
      : undefined,
    refusjonskrav
      ? {
          tittel: "Refusjonskrav",
          lenke: `/tiltaksgjennomforinger/${tiltaksgjennomforingId}/refusjonskrav`,
        }
      : undefined,
  ];
}

export function TiltaksgjennomforingPage() {
  const { pathname } = useLocation();
  const { avtaleId } = useParams();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { tiltaksgjennomforing } = useLoaderData<typeof tiltaksgjennomforingLoader>();
  const { data: enableOpprettTilsagn } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_OPPRETT_TILSAGN,
  );

  if (!tiltaksgjennomforing) {
    return (
      <Alert variant="warning">
        Klarte ikke finne tiltaksgjennomføring
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
  }

  const currentTab = () => {
    if (pathname.includes("tilsagn")) {
      return "tilsagn";
    } else if (pathname.includes("deltakere")) {
      return "poc";
    } else if (pathname.includes("refusjonskrav")) {
      return "refusjonskrav";
    } else {
      return "gjennomforing";
    }
  };

  const brodsmuler = createBrodsmuler(
    tiltaksgjennomforing.id,
    avtaleId,
    currentTab() === "tilsagn",
    currentTab() === "refusjonskrav",
  );

  return (
    <main>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header harForhandsvisningsknapp>
        <div
          className={classNames(
            headerStyles.header_outer_container,
            headerStyles.header_outer_container_forhandsvisningsknapp,
          )}
        >
          <div className={headerStyles.tiltaksnavn_status}>
            <TiltaksgjennomforingIkon />
            <VStack>
              <Heading className={headerStyles.navn} size="large" level="2">
                {tiltaksgjennomforing.navn}
              </Heading>
            </VStack>
            <GjennomforingStatusMedAarsakTag status={tiltaksgjennomforing.status} />
            <DupliserTiltak tiltaksgjennomforing={tiltaksgjennomforing} />
          </div>
          {gjennomforingIsAktiv(tiltaksgjennomforing.status.status) && (
            <div className={headerStyles.forhandsvisningsknapp}>
              <Lenkeknapp
                size="small"
                isExternal={true}
                variant="secondary"
                to={`${PREVIEW_ARBEIDSMARKEDSTILTAK_URL}/tiltak/${tiltaksgjennomforing.id}`}
              >
                Forhåndsvis i Modia
              </Lenkeknapp>
            </div>
          )}
        </div>
      </Header>

      <Tabs value={currentTab()}>
        <Tabs.List className={commonStyles.list}>
          <Tabs.Tab
            value="gjennomforing"
            label="Gjennomføring"
            onClick={() =>
              navigateAndReplaceUrl(`/tiltaksgjennomforinger/${tiltaksgjennomforing.id}`)
            }
            aria-controls="panel"
          />
          {enableOpprettTilsagn ? (
            <>
              <Tabs.Tab
                value="tilsagn"
                label="Tilsagn"
                onClick={() =>
                  navigateAndReplaceUrl(
                    `/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/tilsagn`,
                  )
                }
                aria-controls="panel"
              />
              <Tabs.Tab
                value="refusjonskrav"
                label="Refusjonskrav"
                onClick={() =>
                  navigateAndReplaceUrl(
                    `/tiltaksgjennomforinger/${tiltaksgjennomforing.id}/refusjonskrav`,
                  )
                }
                aria-controls="panel"
              />
            </>
          ) : null}
        </Tabs.List>
        <ContainerLayout>
          <div id="panel">
            <Outlet />
          </div>
        </ContainerLayout>
      </Tabs>
    </main>
  );
}
