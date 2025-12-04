import type { LoaderFunctionArgs, MetaFunction } from "react-router";
import { redirect, useLoaderData } from "react-router";
import { ArrangorDto, ArrangorflateService } from "api-client";
import { apiHeaders } from "~/auth/auth.server";
import { pathByOrgnr } from "~/utils/navigation";
import { problemDetailResponse } from "~/utils/validering";
import css from "../root.module.css";
import { BodyShort, Box, Detail, HGrid, LinkCard, Tag } from "@navikt/ds-react";
import { PageHeading } from "~/components/common/PageHeading";
import { tekster } from "~/tekster";

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for utbetalinger" },
  ];
};

export async function loader({ request }: LoaderFunctionArgs) {
  const { data: arrangorer, error } =
    await ArrangorflateService.getArrangorerInnloggetBrukerHarTilgangTil({
      headers: await apiHeaders(request),
    });

  if (error) {
    throw problemDetailResponse(error);
  }
  if (arrangorer.length === 0) {
    return redirect("/ingen-tilgang");
  }
  return { arrangorer };
}

export default function Index() {
  const { arrangorer } = useLoaderData<typeof loader>();
  return (
    <Box className={css.side}>
      <div className="mb-4">
        <PageHeading title={tekster.bokmal.arrangor.headingTitle} />
        <BodyShort>{tekster.bokmal.arrangor.beskrivelse}</BodyShort>
      </div>
      <HGrid gap="space-16" columns="repeat(auto-fit, minmax(300px, 1fr))">
        {arrangorer.map((arrangor) => (
          <LinkCard key={arrangor.id}>
            <LinkCard.Title>
              <LinkCard.Anchor href={pathByOrgnr(arrangor.organisasjonsnummer).utbetalinger}>
                {arrangor.navn}
              </LinkCard.Anchor>
            </LinkCard.Title>
            <LinkCard.Description>{arrangor.organisasjonsnummer}</LinkCard.Description>
            <LinkCard.Footer>
              <ArrangorTag arrangor={arrangor} />
            </LinkCard.Footer>
          </LinkCard>
        ))}
      </HGrid>
    </Box>
  );
}

interface ArrangorTagProps {
  arrangor: ArrangorDto;
}

function ArrangorTag({ arrangor }: ArrangorTagProps) {
  if (arrangor.overordnetEnhet) {
    <Tag size="small" variant="neutral-filled">
      Underenhet
    </Tag>;
  }
  return (
    <Tag size="small" variant="neutral">
      Hovedenhet
    </Tag>
  );
}
