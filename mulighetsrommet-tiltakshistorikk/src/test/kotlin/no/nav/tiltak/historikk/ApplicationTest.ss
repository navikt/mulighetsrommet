╔═ OpenAPI er tilgjengelig uten autentisering og beskriver kun historikk-endepunktet ═╗
{
  "openapi": "3.1.1",
  "info": {
    "title": "Tiltakshistorikk",
    "version": "v1"
  },
  "paths": {
    "/api/v1/historikk": {
      "post": {
        "summary": "Hent tiltakshistorikk",
        "description": "Henter tiltakshistorikk for ett eller flere fødselsnummer oppgitt i `identer`.\nResultatet er en samlet liste med deltakelser fra Arena, tiltak hos arrangør fra Team Komet\nog tiltak hos arbeidsgiver fra Team Tiltak, der feltet `type` angir deltakelsestypen.\nFor komplett historikk per bruker så må konsumenten hente alle historiske identer fra PDL\nog inkludere dem i forespørselen.",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/TiltakshistorikkV1Request"
              }
            }
          },
          "required": true
        },
        "responses": {
          "200": {
            "description": "Tiltakshistorikk for de oppgitte identene.",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/TiltakshistorikkV1Response"
                }
              }
            }
          },
          "401": {
            "description": "Mangler gyldig tilgang."
          }
        },
        "security": [
          {
            "TILTAKSHISTORIKK_READ": []
          }
        ]
      }
    }
  },
  "components": {
    "schemas": {
      "TiltakshistorikkV1Request": {
        "type": "object",
        "title": "TiltakshistorikkV1Request",
        "required": [
          "identer"
        ],
        "properties": {
          "identer": {
            "type": "array",
            "items": {
              "type": "string"
            }
          }
        }
      },
      "TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype",
        "required": [
          "tiltakskode",
          "navn"
        ],
        "properties": {
          "tiltakskode": {
            "type": "string"
          },
          "navn": {
            "type": "string"
          }
        }
      },
      "TiltakshistorikkV1Dto.Gjennomforing": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto.Gjennomforing",
        "required": [
          "id"
        ],
        "properties": {
          "id": {
            "type": "string"
          },
          "navn": {
            "type": [
              "string",
              "null"
            ]
          },
          "deltidsprosent": {
            "type": [
              "number",
              "null"
            ]
          }
        }
      },
      "TiltakshistorikkV1Dto.Virksomhet": {
        "type": [
          "object",
          "null"
        ],
        "title": "TiltakshistorikkV1Dto.Virksomhet",
        "required": [
          "organisasjonsnummer"
        ],
        "properties": {
          "organisasjonsnummer": {
            "type": "string"
          },
          "navn": {
            "type": [
              "string",
              "null"
            ]
          }
        }
      },
      "TiltakshistorikkV1Dto.Virksomhet2": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto.Virksomhet2",
        "required": [
          "organisasjonsnummer"
        ],
        "properties": {
          "organisasjonsnummer": {
            "type": "string"
          },
          "navn": {
            "type": [
              "string",
              "null"
            ]
          }
        }
      },
      "TiltakshistorikkV1Dto.Arrangor": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto.Arrangor",
        "required": [
          "underenhet"
        ],
        "properties": {
          "hovedenhet": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.Virksomhet"
          },
          "underenhet": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.Virksomhet2"
          }
        }
      },
      "TiltakshistorikkV1Dto.ArenaDeltakelse": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto.ArenaDeltakelse",
        "required": [
          "type",
          "norskIdent",
          "opprettetTidspunkt",
          "oppdatertTidspunkt",
          "id",
          "tittel",
          "arenaId",
          "status",
          "tiltakstype",
          "gjennomforing",
          "arrangor"
        ],
        "properties": {
          "type": {
            "type": "string",
            "enum": [
              "ArenaDeltakelse"
            ]
          },
          "norskIdent": {
            "type": "string"
          },
          "startDato": {
            "type": [
              "string",
              "null"
            ]
          },
          "sluttDato": {
            "type": [
              "string",
              "null"
            ]
          },
          "opprettetTidspunkt": {
            "type": "string"
          },
          "oppdatertTidspunkt": {
            "type": "string"
          },
          "id": {
            "type": "string"
          },
          "tittel": {
            "type": "string"
          },
          "arenaId": {
            "type": "integer"
          },
          "status": {
            "type": "string",
            "enum": [
              "AKTUELL",
              "AVSLAG",
              "DELTAKELSE_AVBRUTT",
              "FEILREGISTRERT",
              "FULLFORT",
              "GJENNOMFORES",
              "GJENNOMFORING_AVBRUTT",
              "GJENNOMFORING_AVLYST",
              "IKKE_AKTUELL",
              "IKKE_MOTT",
              "INFORMASJONSMOTE",
              "TAKKET_JA_TIL_TILBUD",
              "TAKKET_NEI_TIL_TILBUD",
              "TILBUD",
              "VENTELISTE"
            ]
          },
          "tiltakstype": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype"
          },
          "gjennomforing": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.Gjennomforing"
          },
          "arrangor": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.Arrangor"
          },
          "deltidsprosent": {
            "type": [
              "number",
              "null"
            ]
          },
          "dagerPerUke": {
            "type": [
              "number",
              "null"
            ]
          },
          "opphav": {
            "type": "string",
            "enum": [
              "ARENA",
              "TEAM_KOMET",
              "TEAM_TILTAK"
            ]
          }
        }
      },
      "TiltakshistorikkV1Dto.TeamKometDeltakelse.Status": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto.TeamKometDeltakelse.Status",
        "required": [
          "type",
          "opprettetTidspunkt"
        ],
        "properties": {
          "type": {
            "type": "string",
            "enum": [
              "AVBRUTT",
              "AVBRUTT_UTKAST",
              "DELTAR",
              "FEILREGISTRERT",
              "FULLFORT",
              "HAR_SLUTTET",
              "IKKE_AKTUELL",
              "KLADD",
              "PABEGYNT_REGISTRERING",
              "SOKT_INN",
              "UTKAST_TIL_PAMELDING",
              "VENTELISTE",
              "VENTER_PA_OPPSTART",
              "VURDERES"
            ]
          },
          "aarsak": {
            "type": [
              "string",
              "null"
            ],
            "enum": [
              "ANNET",
              "AVLYST_KONTRAKT",
              "FATT_JOBB",
              "FEILREGISTRERT",
              "FERDIG",
              "FIKK_IKKE_PLASS",
              "IKKE_MOTT",
              "OPPFYLLER_IKKE_KRAVENE",
              "SAMARBEIDET_MED_ARRANGOREN_ER_AVBRUTT",
              "SYK",
              "TRENGER_ANNEN_STOTTE",
              "UTDANNING",
              "KRAV_IKKE_OPPFYLT",
              "KURS_FULLT"
            ]
          },
          "opprettetTidspunkt": {
            "type": "string"
          }
        }
      },
      "TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype",
        "required": [
          "tiltakskode",
          "navn"
        ],
        "properties": {
          "tiltakskode": {
            "type": "string",
            "enum": [
              "ARBEIDSRETTET_REHABILITERING",
              "AVKLARING",
              "DIGITALT_OPPFOLGINGSTILTAK",
              "JOBBKLUBB",
              "OPPFOLGING",
              "ARBEIDSFORBEREDENDE_TRENING",
              "VARIG_TILRETTELAGT_ARBEID_SKJERMET",
              "TILRETTELAGT_ARBEID_ORDINAER",
              "ARBEIDSMARKEDSOPPLAERING",
              "ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING",
              "ENKELTPLASS_FAG_OG_YRKESOPPLAERING",
              "FAG_OG_YRKESOPPLAERING",
              "GRUPPE_ARBEIDSMARKEDSOPPLAERING",
              "GRUPPE_FAG_OG_YRKESOPPLAERING",
              "HOYERE_UTDANNING",
              "HOYERE_YRKESFAGLIG_UTDANNING",
              "NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV",
              "STUDIESPESIALISERING"
            ]
          },
          "navn": {
            "type": "string"
          }
        }
      },
      "TiltakshistorikkV1Dto.TeamKometDeltakelse": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto.TeamKometDeltakelse",
        "required": [
          "type",
          "norskIdent",
          "opprettetTidspunkt",
          "oppdatertTidspunkt",
          "id",
          "tittel",
          "status",
          "tiltakstype",
          "gjennomforing",
          "arrangor"
        ],
        "properties": {
          "type": {
            "type": "string",
            "enum": [
              "TeamKometDeltakelse"
            ]
          },
          "norskIdent": {
            "type": "string"
          },
          "startDato": {
            "type": [
              "string",
              "null"
            ]
          },
          "sluttDato": {
            "type": [
              "string",
              "null"
            ]
          },
          "opprettetTidspunkt": {
            "type": "string"
          },
          "oppdatertTidspunkt": {
            "type": "string"
          },
          "id": {
            "type": "string"
          },
          "tittel": {
            "type": "string"
          },
          "status": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.TeamKometDeltakelse.Status"
          },
          "tiltakstype": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype"
          },
          "gjennomforing": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.Gjennomforing"
          },
          "arrangor": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.Arrangor"
          },
          "deltidsprosent": {
            "type": [
              "number",
              "null"
            ]
          },
          "dagerPerUke": {
            "type": [
              "number",
              "null"
            ]
          },
          "opphav": {
            "type": "string",
            "enum": [
              "ARENA",
              "TEAM_KOMET",
              "TEAM_TILTAK"
            ]
          }
        }
      },
      "TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype",
        "required": [
          "tiltakskode",
          "navn"
        ],
        "properties": {
          "tiltakskode": {
            "type": "string",
            "enum": [
              "ARBEIDSTRENING",
              "MIDLERTIDIG_LONNSTILSKUDD",
              "VARIG_LONNSTILSKUDD",
              "MENTOR",
              "INKLUDERINGSTILSKUDD",
              "SOMMERJOBB",
              "VTAO",
              "FIREARIG_LONNSTILSKUDD"
            ]
          },
          "navn": {
            "type": "string"
          }
        }
      },
      "TiltakshistorikkV1Dto.TeamTiltakAvtale": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto.TeamTiltakAvtale",
        "required": [
          "type",
          "norskIdent",
          "opprettetTidspunkt",
          "oppdatertTidspunkt",
          "id",
          "tittel",
          "tiltakstype",
          "status",
          "arbeidsgiver"
        ],
        "properties": {
          "type": {
            "type": "string",
            "enum": [
              "TeamTiltakAvtale"
            ]
          },
          "norskIdent": {
            "type": "string"
          },
          "startDato": {
            "type": [
              "string",
              "null"
            ]
          },
          "sluttDato": {
            "type": [
              "string",
              "null"
            ]
          },
          "opprettetTidspunkt": {
            "type": "string"
          },
          "oppdatertTidspunkt": {
            "type": "string"
          },
          "id": {
            "type": "string"
          },
          "tittel": {
            "type": "string"
          },
          "tiltakstype": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.TeamTiltakAvtale.Tiltakstype"
          },
          "status": {
            "type": "string",
            "enum": [
              "PAABEGYNT",
              "MANGLER_GODKJENNING",
              "KLAR_FOR_OPPSTART",
              "GJENNOMFORES",
              "AVSLUTTET",
              "AVBRUTT",
              "ANNULLERT"
            ]
          },
          "stillingsprosent": {
            "type": [
              "number",
              "null"
            ]
          },
          "dagerPerUke": {
            "type": [
              "number",
              "null"
            ]
          },
          "arbeidsgiver": {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.Virksomhet2"
          },
          "opphav": {
            "type": "string",
            "enum": [
              "ARENA",
              "TEAM_KOMET",
              "TEAM_TILTAK"
            ]
          }
        }
      },
      "TiltakshistorikkV1Dto": {
        "type": "object",
        "title": "TiltakshistorikkV1Dto",
        "oneOf": [
          {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.ArenaDeltakelse"
          },
          {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.TeamKometDeltakelse"
          },
          {
            "$ref": "#/components/schemas/TiltakshistorikkV1Dto.TeamTiltakAvtale"
          }
        ],
        "discriminator": {
          "propertyName": "type",
          "mapping": {
            "ArenaDeltakelse": "#/components/schemas/TiltakshistorikkV1Dto.ArenaDeltakelse",
            "TeamKometDeltakelse": "#/components/schemas/TiltakshistorikkV1Dto.TeamKometDeltakelse",
            "TeamTiltakAvtale": "#/components/schemas/TiltakshistorikkV1Dto.TeamTiltakAvtale"
          }
        }
      },
      "TiltakshistorikkV1Response": {
        "type": "object",
        "title": "TiltakshistorikkV1Response",
        "required": [
          "historikk"
        ],
        "properties": {
          "historikk": {
            "type": "array",
            "items": {
              "$ref": "#/components/schemas/TiltakshistorikkV1Dto"
            }
          }
        }
      }
    },
    "securitySchemes": {
      "TILTAKSHISTORIKK_READ": {
        "scheme": "bearer",
        "bearerFormat": "JWT",
        "description": "JWT Bearer Authentication",
        "type": "http"
      },
      "TILTAKSHISTORIKK_WRITE": {
        "scheme": "bearer",
        "bearerFormat": "JWT",
        "description": "JWT Bearer Authentication",
        "type": "http"
      },
      "TEAM_MULIGHETSROMMET": {
        "scheme": "bearer",
        "bearerFormat": "JWT",
        "description": "JWT Bearer Authentication",
        "type": "http"
      }
    }
  }
}
╔═ [end of file] ═╗
