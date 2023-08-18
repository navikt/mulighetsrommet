import { useClient } from "sanity";
import { API_VERSION } from "../sanity.config";

const INDIVUDUELLE_TILTAK = [
  "02509279-0a0f-4bd6-b506-f40111e4ba14", // "VTA - varig tilrettelagt arbeid i skjermet virksomhet",
  "18ff4bef-f62e-444a-920f-e30bde5c3950", // "Tilskudd til sommerjobb",
  "2ba9c085-3780-420a-a5d5-820788c74d29", //  Inkluderingstilskudd,
  "4457d760-81a4-4c16-8ab3-64c72d424db2", // Opplæring - Høyere utdanning",
  "5328120c-028b-4ede-8250-ebf22536b021", // "Mentor",
  "6c4f372f-9631-4916-b7c1-549c17239d78", // "Opplæring - Enkeltplass AMO",
  "Opplæring - Enkeltplass Fag- og yrkesopplæring eller fagskole",
  "9911dbcb-b67f-408f-9c0c-4d7f67a863d8", // "Varig lønnstilskudd",
  "Opplæring - Fagskole (høyere yrkesfaglig utdanning)",
  "d1521b2f-589b-4101-ae11-bd555314b905", // "Midlertidig lønnstilskudd",
  "bab45555-4631-4e5e-9a17-365fc7b335de", // "Arbeidstrening",
];
| Enkeltplass Fag- og yrkesopplæring VGS og høyere yrkesfaglig utdanning | 6f46bd0b-c9a7-4b03-bd16-e51a8f80f88d |
| Avklaring                                                              | f9618e97-4510-49e2-b748-29cae84d9019 |
| Digitalt oppfølgingstiltak for arbeidsledige (jobbklubb)               | 3526de0d-ad4c-4b81-b072-a13b3a4b4ed3 |
| Oppfølging                                                             | 5ac48c03-1f4c-4d4b-b862-050caca92080 |
| Gruppe AMO                                                             | eadeb22c-bd89-4298-a5c2-145f112f8e7d |
| Arbeidsrettet rehabilitering (dag)                                     | 29c3d3cb-ffbf-4c22-8ffc-fea5d7f6c822 |
| Jobbklubb                                                              | 31e72dd8-ad05-4e81-a7f9-fd4c8f295864 |
| Varig tilrettelagt arbeid i ordinær virksomhet                         | 661e79e6-721b-452c-a6d4-8c71493f15e3 |
| Utvidet oppfølging i NAV                                               | 9fbf9feb-aa4c-4e3c-bc0e-edee0ab957ad |

export const isIndividueltTiltak = async (tiltakstypeRef: string) => {
  //const client = useClient({ apiVersion: API_VERSION });

  //const res = await client.fetch(
    //"*[_type == 'tiltakstype' && _id == $ref][0]",
    //{ ref: tiltakstypeRef },
  //);
  //console.log(13, tiltakstypeRef, res);

  //return INDIVUDUELLE_TILTAK.includes(res.tiltakstypeNavn);
  return false;
}
