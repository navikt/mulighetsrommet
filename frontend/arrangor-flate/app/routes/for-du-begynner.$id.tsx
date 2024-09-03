import { BodyShort, GuidePanel, Heading, List } from "@navikt/ds-react";
import { PageHeader } from "../components/PageHeader";
import { json, Link, MetaFunction, useParams } from "@remix-run/react";

export const meta: MetaFunction = () => {
  return [
    { title: "Før du begynner" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

export function loader() {
  return json({});
}

export default function ForDuBegynner() {
  const { id } = useParams();
  return (
    <>
      <PageHeader
        title="Før du begynner"
        tilbakeLenke={{ navn: "Tilbake til refusjonskravliste", url: "/" }}
      />
      <div className="grid gap-5 flex-col content-center">
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
        <Link className="justify-self-end" to={`/deltakerliste/${id}`}>
          Neste
        </Link>
      </div>
    </>
  );
}
