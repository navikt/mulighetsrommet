╔═ avtalt pris per time oppfølging per deltaker/journalpostTimesPris ═╗
{
  "title": "Utbetaling",
  "subject": "Krav om utbetaling fra Nav",
  "description": "Krav om utbetaling fra Nav",
  "author": "Tiltaksadministrasjon",
  "enhet": null,
  "sections": [
    {
      "title": {
        "text": "Innsendt krav om utbetaling",
        "level": 1
      }
    },
    {
      "title": {
        "text": "Innsending",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Arrangør",
              "value": "Nav (123456789)"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Dato innsendt av arrangør",
              "value": "2025-01-02",
              "format": "DATE"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Tiltakstype",
              "value": "Oppfolging"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Løpenummer",
              "value": "2025/10000"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Utbetaling",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Utbetalingsperiode",
              "value": "01.01.2025 - 31.01.2025"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Utbetales tidligst",
              "value": null,
              "format": "DATE"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Stengt periode",
              "value": "07.01.2025 - 13.01.2025"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Avtalt pris per time oppfølging",
              "value": "34",
              "currency": "NOK"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Beløp",
              "value": "100",
              "currency": "NOK"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Betalingsinformasjon",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Kontonummer",
              "value": "12345678901"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "KID-nummer",
              "value": null
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Stengt hos arrangør",
        "level": 2
      },
      "blocks": [
        {
          "type": "item-list",
          "description": "Det er registrert stengt hos arrangør i følgende perioder:",
          "items": [
            "07.01.2025 - 13.01.2025: Stengt for ferie"
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Deltakerperioder",
        "level": 2
      },
      "blocks": [
        {
          "type": "table",
          "table": {
            "columns": [
              {
                "title": "Navn"
              },
              {
                "title": "Fødselsnr.",
                "align": "RIGHT"
              },
              {
                "title": "Startdato i perioden",
                "align": "RIGHT"
              },
              {
                "title": "Sluttdato i perioden",
                "align": "RIGHT"
              }
            ],
            "rows": [
              {
                "cells": [
                  {
                    "value": "Skjermet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Ola Nordmann"
                  },
                  {
                    "value": "01010199999"
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  }
                ]
              }
            ]
          }
        }
      ]
    }
  ]
}
╔═ avtalt pris per time oppfølging per deltaker/utbetalingsdetaljerTimesPris ═╗
{
  "title": "Utbetalingsdetaljer",
  "subject": "Utbetaling til Nav",
  "description": "Detaljer om utbetaling for gjennomføring av Oppfolging",
  "author": "Nav",
  "enhet": null,
  "sections": [
    {
      "title": {
        "text": "Detaljer om utbetaling",
        "level": 1
      }
    },
    {
      "title": {
        "text": "Innsending",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Arrangør",
              "value": "Nav (123456789)"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Dato innsendt av arrangør",
              "value": "2025-01-02",
              "format": "DATE"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Tiltakstype",
              "value": "Oppfolging"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Løpenummer",
              "value": "2025/10000"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Utbetaling",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Utbetalingsperiode",
              "value": "01.01.2025 - 31.01.2025"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Utbetales tidligst",
              "value": null,
              "format": "DATE"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Stengt periode",
              "value": "07.01.2025 - 13.01.2025"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Avtalt pris per time oppfølging",
              "value": "34",
              "currency": "NOK"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Beløp",
              "value": "100",
              "currency": "NOK"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Betalingsinformasjon",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Kontonummer",
              "value": "12345678901"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "KID-nummer",
              "value": null
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Utbetalingsstatus",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Status",
              "value": "Overført til utbetaling",
              "format": "STATUS_SUCCESS"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Godkjent beløp til utbetaling",
              "value": "100",
              "currency": "NOK"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Tilsagn som er brukt til utbetaling",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Tilsagn",
              "value": "A-1-1"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Beløp til utbetaling",
              "value": "99",
              "currency": "NOK"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Status",
              "value": "Overført til utbetaling",
              "format": "STATUS_SUCCESS"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Status endret",
              "value": "2025-01-03T00:00",
              "format": "DATE"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Tilsagn",
              "value": "A-1-2"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Beløp til utbetaling",
              "value": "1",
              "currency": "NOK"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Status",
              "value": "Overført til utbetaling",
              "format": "STATUS_SUCCESS"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Status endret",
              "value": "2025-01-03T00:00",
              "format": "DATE"
            }
          ]
        }
      ]
    }
  ]
}
╔═ fast sats per tiltaksplass per maned/journalpostFastSats ═╗
{
  "title": "Utbetaling",
  "subject": "Krav om utbetaling fra Nav",
  "description": "Krav om utbetaling fra Nav",
  "author": "Tiltaksadministrasjon",
  "enhet": null,
  "sections": [
    {
      "title": {
        "text": "Innsendt krav om utbetaling",
        "level": 1
      }
    },
    {
      "title": {
        "text": "Innsending",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Arrangør",
              "value": "Nav (123456789)"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Dato innsendt av arrangør",
              "value": "2025-01-02",
              "format": "DATE"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Tiltakstype",
              "value": "Oppfølging"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Løpenummer",
              "value": "2025/10000"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Utbetaling",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Utbetalingsperiode",
              "value": "01.01.2025 - 31.01.2025"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Utbetales tidligst",
              "value": null,
              "format": "DATE"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Stengt periode",
              "value": "07.01.2025 - 13.01.2025"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Sats",
              "value": "1000",
              "currency": "NOK"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Antall månedsverk",
              "value": "4.25"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Beløp",
              "value": "100",
              "currency": "NOK"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Betalingsinformasjon",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Kontonummer",
              "value": "12345678901"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "KID-nummer",
              "value": null
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Stengt hos arrangør",
        "level": 2
      },
      "blocks": [
        {
          "type": "item-list",
          "description": "Det er registrert stengt hos arrangør i følgende perioder:",
          "items": [
            "07.01.2025 - 13.01.2025: Stengt for ferie"
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Deltakerperioder",
        "level": 2
      },
      "blocks": [
        {
          "type": "table",
          "table": {
            "columns": [
              {
                "title": "Navn"
              },
              {
                "title": "Fødselsnr.",
                "align": "RIGHT"
              },
              {
                "title": "Startdato i perioden",
                "align": "RIGHT"
              },
              {
                "title": "Sluttdato i perioden",
                "align": "RIGHT"
              },
              {
                "title": "Deltakelsesprosent",
                "align": "RIGHT"
              }
            ],
            "rows": [
              {
                "cells": [
                  {
                    "value": "Skjermet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  },
                  {
                    "value": "100.0",
                    "format": "PERCENT"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Ola Nordmann"
                  },
                  {
                    "value": "01010199999"
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  },
                  {
                    "value": "100.0",
                    "format": "PERCENT"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "14.01.2025"
                  },
                  {
                    "value": "50.0",
                    "format": "PERCENT"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "15.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  },
                  {
                    "value": "100.0",
                    "format": "PERCENT"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "14.01.2025"
                  },
                  {
                    "value": "50.0",
                    "format": "PERCENT"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "15.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  },
                  {
                    "value": "100.0",
                    "format": "PERCENT"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "01.01.2025"
                  },
                  {
                    "value": "14.01.2025"
                  },
                  {
                    "value": "50.0",
                    "format": "PERCENT"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "15.01.2025"
                  },
                  {
                    "value": "31.01.2025"
                  },
                  {
                    "value": "100.0",
                    "format": "PERCENT"
                  }
                ]
              }
            ]
          }
        }
      ]
    },
    {
      "title": {
        "text": "Beregnet månedsverk",
        "level": 2
      },
      "blocks": [
        {
          "type": "table",
          "table": {
            "columns": [
              {
                "title": "Navn"
              },
              {
                "title": "Fødselsnr.",
                "align": "RIGHT"
              },
              {
                "title": "Månedsverk",
                "align": "RIGHT"
              }
            ],
            "rows": [
              {
                "cells": [
                  {
                    "value": "Skjermet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "1.0"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Ola Nordmann"
                  },
                  {
                    "value": "01010199999"
                  },
                  {
                    "value": "1.0"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "0.75"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "0.75"
                  }
                ]
              },
              {
                "cells": [
                  {
                    "value": "Adressebeskyttet"
                  },
                  {
                    "value": null
                  },
                  {
                    "value": "0.75"
                  }
                ]
              }
            ]
          }
        }
      ]
    }
  ]
}
╔═ fast sats per tiltaksplass per maned/utbetalingsdetaljerFastSats ═╗
{
  "title": "Utbetalingsdetaljer",
  "subject": "Utbetaling til Nav",
  "description": "Detaljer om utbetaling for gjennomføring av Oppfølging",
  "author": "Nav",
  "enhet": null,
  "sections": [
    {
      "title": {
        "text": "Detaljer om utbetaling",
        "level": 1
      }
    },
    {
      "title": {
        "text": "Innsending",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Arrangør",
              "value": "Nav (123456789)"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Dato innsendt av arrangør",
              "value": "2025-01-02",
              "format": "DATE"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Tiltakstype",
              "value": "Oppfølging"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Løpenummer",
              "value": "2025/10000"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Utbetaling",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Utbetalingsperiode",
              "value": "01.01.2025 - 31.01.2025"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Utbetales tidligst",
              "value": null,
              "format": "DATE"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Stengt periode",
              "value": "07.01.2025 - 13.01.2025"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Sats",
              "value": "1000",
              "currency": "NOK"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Antall månedsverk",
              "value": "4.25"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Beløp",
              "value": "100",
              "currency": "NOK"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Betalingsinformasjon",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Kontonummer",
              "value": "12345678901"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "KID-nummer",
              "value": null
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Utbetalingsstatus",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Status",
              "value": "Overført til utbetaling",
              "format": "STATUS_SUCCESS"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Godkjent beløp til utbetaling",
              "value": "100",
              "currency": "NOK"
            }
          ]
        }
      ]
    },
    {
      "title": {
        "text": "Tilsagn som er brukt til utbetaling",
        "level": 2
      },
      "blocks": [
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Tilsagn",
              "value": "A-1-1"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Beløp til utbetaling",
              "value": "99",
              "currency": "NOK"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Status",
              "value": "Overført til utbetaling",
              "format": "STATUS_SUCCESS"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Status endret",
              "value": "2025-01-03T00:00",
              "format": "DATE"
            }
          ]
        },
        {
          "type": "description-list",
          "entries": [
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Tilsagn",
              "value": "A-1-2"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.MoneyAmount",
              "label": "Beløp til utbetaling",
              "value": "1",
              "currency": "NOK"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Status",
              "value": "Overført til utbetaling",
              "format": "STATUS_SUCCESS"
            },
            {
              "type": "no.nav.mulighetsrommet.api.pdfgen.DescriptionListBlock.Entry.Text",
              "label": "Status endret",
              "value": "2025-01-03T00:00",
              "format": "DATE"
            }
          ]
        }
      ]
    }
  ]
}
╔═ [end of file] ═╗
