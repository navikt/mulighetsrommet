import { ActionMenu, BodyShort, Button, VStack } from "@navikt/ds-react";
import { useNavigate, useParams } from "react-router";
import { ArrangorflateArrangor } from "api-client";
import { pathByOrgnr } from "~/utils/navigation";
import { ChevronDownIcon } from "@navikt/aksel-icons";

interface Props {
  arrangorer: ArrangorflateArrangor[];
}

export function Arrangorvelger({ arrangorer }: Props) {
  const navigate = useNavigate();
  const { orgnr } = useParams();
  const arrangor = arrangorer.find((a) => a.organisasjonsnummer === orgnr);

  return (
    <ActionMenu>
      <ActionMenu.Trigger>
        <Button
          variant="secondary-neutral"
          icon={<ChevronDownIcon aria-hidden />}
          iconPosition="right"
        >
          <VStack align="start">
            <BodyShort size="small">{arrangor?.navn}</BodyShort>
            <BodyShort size="small">{orgnr}</BodyShort>
          </VStack>
        </Button>
      </ActionMenu.Trigger>
      <ActionMenu.Content>
        <VStack className="max-h-[500px] overflow-y-scroll">
          {arrangorer.map((arrangor) => (
            <ActionMenu.Item
              key={arrangor.id}
              onSelect={() => navigate(pathByOrgnr(arrangor.organisasjonsnummer).utbetalinger)}
            >
              <BodyShort size="small">
                {`${arrangor.navn} - ${arrangor.organisasjonsnummer}`}
              </BodyShort>
            </ActionMenu.Item>
          ))}
        </VStack>
      </ActionMenu.Content>
    </ActionMenu>
  );
}
