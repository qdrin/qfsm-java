CREATE EXTENSION pgcrypto;

CREATE TABLE IF NOT EXISTS process_config
(
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    service_id VARCHAR NOT NULL,
    action VARCHAR NOT NULL,
    partnership_id VARCHAR NOT NULL,
    scenarios JSON NOT NULL,
    validate_config JSON,
    extra_params JSON
);
CREATE UNIQUE INDEX service_id_action_idx ON process_config (service_id, action);

CREATE TABLE IF NOT EXISTS callback_config
(
    client_id VARCHAR PRIMARY KEY,
    url VARCHAR,
    method VARCHAR,
    retries JSON,
    connector_config JSON,
    extra_params JSON
);

insert into process_config 
  (service_id, action, partnership_id, scenarios, validate_config) values 
  (
    'fpc_partner1_offer1',
    'check',
    'partner1',
    '[
      {"name": "MapMsisdnToMsisdn", "config": {}},
      {"name": "MapServiceIdDefault", "config": {}},
      {
        "name": "SyncPartnerDefault",
        "config": {
          "url": "http://{mockHost}:{mockPort}/eapi/factory-adapter-bss-connector/v1/availability",
          "method": "POST",
          "connectorConfig": {
            "command": {
              "url": "http://{mockHost}:{mockPort}/v1/partner1/availability",
              "method": "POST"
            }
          }
        }
      }
    ]',
    '{
      "required": {
        "serviceCharacteristics": {
          "price": {}
        },
        "partnerContext": {
          "partnerServices": {
            "maxSize": 1
          }
        }
      }
    }'
  );

insert into process_config 
  (service_id, action, partnership_id, scenarios, validate_config) values 
  (
    'fpc_partner1_offer1_fake',
    'check',
    'partner1',
    '[
      {"name": "MapMsisdnToMsisdn", "config": {}},
      {"name": "MapServiceIdDefault", "config": {}},
      {
        "name": "SyncPartnerDefault",
        "config": {
          "url": "http://{mockHost}:8082/eapi/factory-adapter-bss-connector/v1/availability",
          "method": "POST",
          "connectorConfig": {
            "command": {
              "url": "http://{mockHost}:{mockPort}/v1/partner1/availability",
              "method": "POST"
            }
          }
        }
      }
    ]',
    '{
      "required": {
        "serviceCharacteristics": {
          "price": {}
        }
      }
    }'
  );

insert into process_config
(service_id, action, partnership_id, scenarios, validate_config) values
    (
        'fpc_partner1_offer1',
        'availability',
        'fpc',
        '[
          {
            "name": "SyncPartnerDefault",
            "config": {
              "requestStarter": "StartFpcGroupRequest",
              "responseParser": "ParseFpcGroupResponse",
              "url": "http://{mockHost}:{mockPort}/eapi/factory-adapter-bss-connector/v1/fpcGroup",
              "method": "POST",
              "connectorConfig": {
                "command": {
                  "url": "http://{mockHost}:{mockPort}/v1/fpc/group",
                  "method": "POST"
                }
              }
            }
          },
          {
            "name": "SyncPartnerDefault",
            "config": {
              "requestStarter": "StartFpcOfferRequest",
              "responseParser": "ParseFpcOfferResponse",
              "url": "http://${ISTIO_HOST}:{mockPort}/eapi/factory-adapter-bss-connector/v1/fpcOffers",
              "method": "POST",
              "connectorConfig": {
                "command": {
                  "url": "http://{mockHost}:{mockPort}/v1/fpc/offers",
                  "method": "POST"
                }
              }
            }
          },
          {
            "name": "SyncPartnerDefault",
            "config": {
              "requestStarter": "StartPoqRequest",
              "responseParser": "ParsePoqResponse",
              "url": "http://{mockHost}:{mockPort}/eapi/factory-adapter-bss-connector/v1/poq",
              "method": "POST",
              "connectorConfig": {
                "command": {
                  "url": "http://{mockHost}:{mockPort}/v1/poq",
                  "method": "POST"
                }
              }
            }
          }
        ]',
        '{}'
    );

    insert into process_config
    (service_id, action, partnership_id, scenarios, validate_config) values
    (
        'productOfferingId_bss_multimapper',
        'CHECKPRICEVALUE',
        'bss',
        '[
          {
            "name": "SyncPartnerDefault",
              "config": {
                "action": "CHECKPRICEVALUE",
                "requestStarter": "StartCheckPriceRequest",
                "responseParser": "ParseCheckPriceResponse",
                "url":  "http://{mockHost}:{mockPort}/eapi/factory-adapter-bss-connector/v1/price",
                "method": "POST",
                "connectorConfig": {
                                                   "command": {
                                                     "url": "http://{mockHost}:{mockPort}/v1/price",
                                                     "method": "POST"
                                                   }
              }
            }
            }
        ]',
        '{}'
    );

