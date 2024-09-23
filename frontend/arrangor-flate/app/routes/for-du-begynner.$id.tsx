import { Button, GuidePanel, Heading, HGrid, List } from "@navikt/ds-react";
import { json, Link, MetaFunction, useParams } from "@remix-run/react";
import { PageHeader } from "../components/PageHeader";
import { requirePersonIdent } from "../auth/auth.server";
import { LoaderFunction } from "@remix-run/node";

export const meta: MetaFunction = () => {
  return [
    { title: "Før du begynner" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

export const loader: LoaderFunction = async ({ request, params }): Promise<object> => {
  await requirePersonIdent(request);
  if (params.id === undefined) throw Error("Mangler id");
  return json({});
};

export default function ForDuBegynner() {
  const { id } = useParams();
  return (
    <>
      <PageHeader
        title="Før du begynner"
        tilbakeLenke={{ navn: "Tilbake til refusjonskravliste", url: "/" }}
      />
      <HGrid columns={1} gap="5">
        <GuidePanel poster>
          <Heading level="2" size="small">
            Vær oppmerksom på dette før du begynner å fylle ut skjemaet
          </Heading>
          <List>
            <List.Item>Man skal ikke plage andre</List.Item>
            <List.Item>Man skal være grei og snill</List.Item>
            <List.Item>Og for øvrig kan man gjøre som man vil</List.Item>
          </List>
        </GuidePanel>
        <Button as={Link} className="justify-self-end" to={`/deltakerliste/${id}`}>
          Neste
        </Button>
      </HGrid>
    </>
  );
}
