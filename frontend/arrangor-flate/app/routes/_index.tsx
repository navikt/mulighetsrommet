import type { MetaFunction } from "@remix-run/node";
import { PageHeader } from "../components/PageHeader";
import { OversiktOverRefusjonskrav } from "../components/refusjonskrav/OversiktOverRefusjonskrav";
import { Krav, KravStatus } from "../domene/domene";

const mockKrav: Krav[] = [
  {
    id: "6",
    belop: "308 530",
    fristForGodkjenning: "31.08.2024",
    kravnr: "6",
    periode: "01.06.2024 - 30.06.2024",
    status: KravStatus.KlarForInnsending,
    tiltaksnr: "2024/123456",
  },
  {
    id: "5",
    belop: "123 000",
    fristForGodkjenning: "31.07.2024",
    kravnr: "5",
    periode: "01.05.2024 - 31.05.2024",
    status: KravStatus.NarmerSegFrist,
    tiltaksnr: "2024/123456",
  },
  {
    id: "4",
    belop: "85 000",
    fristForGodkjenning: "30.06.2024",
    kravnr: "4",
    periode: "01.01.2024 - 31.01.2024",
    status: KravStatus.Attestert,
    tiltaksnr: "2024/123456",
  },
];

export const meta: MetaFunction = () => {
  return [
    { title: "Arrangørflate" },
    { name: "description", content: "Arrangørflate for refusjon" },
  ];
};

export default function Refusjon() {
  return (
    <div className="font-sans p-4">
      <PageHeader title="Tilgjengelige refusjonskrav" />
      <OversiktOverRefusjonskrav krav={mockKrav} />
    </div>
  );
}
