import styles from "./Notater.module.scss";
import { BodyLong, BodyShort } from "@navikt/ds-react";

export default function Notatkort() {
  const notatkortListe = [
    {
      id: "1",
      opprettetAv: "Tania Holst",
      opprettetDato: "2023-01-19 14:03:07",
      innhold:
        "Sed mauris diam, convallis a malesuada a, varius a urna. Interdum et malesuada fames ac ante ipsum primis in faucibus. Donec ac sem mi. Cras vitae sodales odio, sed tincidunt enim. Nunc interdum, dolor viverra consequat fermentum, eros est cursus felis, et convallis lectus orci id urna. Quisque laoreet blandit luctus. Morbi a odio iaculis, porta magna fringilla, luctus elit. Nunc efficitur, enim et efficitur gravida, est diam consectetur odio, non bibendum nibh mauris a augue.",
      avtaleId: "1",
    },
    {
      id: "2",
      opprettetAv: "Veileder veiledersen",
      opprettetDato: "2023-01-19 14:03:07",
      innhold:
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus varius varius turpis. Proin et sapien nisi. Nullam rutrum vitae sem a malesuada. Nullam in facilisis est. Pellentesque nec diam mauris. Nunc ex ligula, commodo et lacus nec, pharetra egestas nibh. Fusce ante sem, fringilla nec dolor vitae, accumsan aliquam tortor. Nullam vulputate molestie nisi sit amet efficitur. Curabitur vestibulum sapien ac leo dapibus, in egestas ante ornare. Maecenas ac viverra velit, ut ultricies magna. Nullam sit amet scelerisque sapien, quis viverra lectus. Donec eu orci venenatis, interdum nunc id, ultricies magna. Aliquam erat volutpat. Phasellus fringilla in nulla ut pretium.\n" +
        "\n" +
        "Duis aliquam non mauris a hendrerit. Maecenas aliquam quis odio in vestibulum. Donec sed vestibulum ligula, tempus lacinia purus. Nunc semper dapibus diam, ac eleifend lacus faucibus ut. Vestibulum nec fringilla nunc, a commodo ligula. Quisque at neque quis turpis dignissim tincidunt. Donec ut erat diam.",
      avtaleId: "2",
    },
  ];

  return (
    <div className={styles.notatkortliste}>
      {notatkortListe.map((notatkort, id) => {
        return (
          <div className={styles.notatkort} key={id}>
            <span className={styles.notatinformasjon}>
              <BodyShort>Lagt til av: {notatkort.opprettetAv}</BodyShort>
              <BodyShort>{notatkort.opprettetDato}</BodyShort>
            </span>
            <BodyLong>{notatkort.innhold}</BodyLong>
          </div>
        );
      })}
    </div>
  );
}
