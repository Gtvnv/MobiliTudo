-- Carga inicial com os quatro locais que o mapa ja exibia (antigo locais.json) e um
-- recorte demonstrativo da malha viaria entre o Palacio das Artes e o Litoral Plaza.
--
-- A malha e deliberadamente pequena e tem uma barreira real no caminho curto: e o que
-- permite ver a rota acessivel divergir da rota mais curta logo na primeira execucao.
-- Os demais locais ficam fora do recorte mapeado de proposito, para exercitar tambem a
-- resposta ORIGEM_FORA_DA_AREA_MAPEADA.
--
-- comprimento_metros fica NULL: o dominio deriva o comprimento da distancia geodesica
-- entre os nos, o que mantem comprimento e geometria sempre coerentes entre si.

-- ---------------------------------------------------------------------------
-- Locais
-- ---------------------------------------------------------------------------

INSERT INTO local (id, nome, categoria, localizacao, endereco, descricao, imagem_url, atualizado_em) VALUES
('a1000000-0000-4000-8000-000000000001', 'Litoral Plaza Shopping', 'SHOPPING',
 ST_SetSRID(ST_MakePoint(-46.407, -23.993), 4326),
 'Av. Ayrton Senna da Silva, 1511 - Vila Caiçara, Praia Grande - SP',
 'Acesso total, banheiros adaptados amplos e vagas reservadas próximas às entradas.',
 'https://fastly.4sqi.net/img/general/600x600/53733052_LwK-9lZJ0E2Z-n7L7_m9uG_mPz7B5_m8Y9Z-n7L7_m9uG.jpg',
 now()),

('a1000000-0000-4000-8000-000000000002', 'Praia Acessível - Guilhermina', 'PRAIA',
 ST_SetSRID(ST_MakePoint(-46.398, -24.015), 4326),
 'Av. Pres. Costa e Silva - Guilhermina, Praia Grande - SP',
 'Equipado com cadeiras anfíbias e esteiras de acesso direto à areia.',
 'https://www.praiagrande.sp.gov.br/arquivos/foto/60111.jpg',
 now()),

('a1000000-0000-4000-8000-000000000003', 'Palácio das Artes', 'CULTURA',
 ST_SetSRID(ST_MakePoint(-46.411, -23.992), 4326),
 'Av. Pres. Costa e Silva, 1600 - Boqueirão, Praia Grande - SP',
 'Rampas de acesso, elevadores modernos e assentos reservados para PCD.',
 'https://turismo.praiagrande.sp.gov.br/arquivos/turismo_pontos/2_1.jpg',
 now()),

('a1000000-0000-4000-8000-000000000004', 'Terminal Tude Bastos', 'TRANSPORTE',
 ST_SetSRID(ST_MakePoint(-46.415, -23.998), 4326),
 'Av. Pres. Kennedy - Boqueirão, Praia Grande - SP',
 'Plataformas com elevadores, piso tátil e banheiros adaptados.',
 'https://praiagrande.sp.gov.br/arquivos/foto/42111.jpg',
 now());

INSERT INTO local_recurso (local_id, recurso) VALUES
('a1000000-0000-4000-8000-000000000001', 'RAMPA'),
('a1000000-0000-4000-8000-000000000001', 'ELEVADOR'),
('a1000000-0000-4000-8000-000000000001', 'PISO_TATIL'),
('a1000000-0000-4000-8000-000000000001', 'BANHEIRO_ADAPTADO'),
('a1000000-0000-4000-8000-000000000001', 'VAGA_PCD'),

('a1000000-0000-4000-8000-000000000002', 'RAMPA'),
('a1000000-0000-4000-8000-000000000002', 'PISO_TATIL'),
('a1000000-0000-4000-8000-000000000002', 'CADEIRA_ANFIBIA'),

('a1000000-0000-4000-8000-000000000003', 'RAMPA'),
('a1000000-0000-4000-8000-000000000003', 'ELEVADOR'),
('a1000000-0000-4000-8000-000000000003', 'PLACA_BRAILLE'),
('a1000000-0000-4000-8000-000000000003', 'ATENDIMENTO_PRIORITARIO'),

('a1000000-0000-4000-8000-000000000004', 'ELEVADOR'),
('a1000000-0000-4000-8000-000000000004', 'PISO_TATIL'),
('a1000000-0000-4000-8000-000000000004', 'PLACA_BRAILLE'),
('a1000000-0000-4000-8000-000000000004', 'BANHEIRO_ADAPTADO');

-- ---------------------------------------------------------------------------
-- Malha viaria: avenida ao norte, rua paralela ao sul e as travessias entre elas
-- ---------------------------------------------------------------------------

INSERT INTO no_via (id, localizacao) VALUES
('av-1',  ST_SetSRID(ST_MakePoint(-46.4110, -23.9920), 4326)),
('av-2',  ST_SetSRID(ST_MakePoint(-46.4100, -23.9922), 4326)),
('av-3',  ST_SetSRID(ST_MakePoint(-46.4090, -23.9924), 4326)),
('av-4',  ST_SetSRID(ST_MakePoint(-46.4080, -23.9926), 4326)),
('av-5',  ST_SetSRID(ST_MakePoint(-46.4070, -23.9930), 4326)),
('par-2', ST_SetSRID(ST_MakePoint(-46.4100, -23.9932), 4326)),
('par-3', ST_SetSRID(ST_MakePoint(-46.4090, -23.9934), 4326)),
('par-4', ST_SetSRID(ST_MakePoint(-46.4080, -23.9936), 4326));

INSERT INTO segmento_via (id, nome_via, no_origem_id, no_destino_id, inclinacao_percentual,
                          largura_metros, superficie, possui_piso_tatil, possui_rebaixamento_guia,
                          travessia, iluminacao_adequada, bidirecional, tracado) VALUES

-- Avenida Presidente Costa e Silva: calcada larga, com piso tatil em quase toda a extensao.
('av-1-2', 'Av. Pres. Costa e Silva', 'av-1', 'av-2', 0.0, 2.40, 'CONCRETO', TRUE, TRUE, FALSE, TRUE, TRUE,
 ST_SetSRID(ST_MakeLine(ST_MakePoint(-46.4110, -23.9920), ST_MakePoint(-46.4100, -23.9922)), 4326)),

('av-2-3', 'Av. Pres. Costa e Silva', 'av-2', 'av-3', 1.5, 2.00, 'CONCRETO', TRUE, TRUE, FALSE, TRUE, TRUE,
 ST_SetSRID(ST_MakeLine(ST_MakePoint(-46.4100, -23.9922), ST_MakePoint(-46.4090, -23.9924)), 4326)),

-- Trecho com obra: a faixa livre caiu para 0,90 m, abaixo do minimo da NBR 9050.
-- E aqui que a rota de cadeira de rodas e obrigada a contornar.
('av-3-4', 'Av. Pres. Costa e Silva', 'av-3', 'av-4', 1.5, 0.90, 'CONCRETO', FALSE, TRUE, FALSE, TRUE, TRUE,
 ST_SetSRID(ST_MakeLine(ST_MakePoint(-46.4090, -23.9924), ST_MakePoint(-46.4080, -23.9926)), 4326)),

('av-4-5', 'Av. Pres. Costa e Silva', 'av-4', 'av-5', 0.0, 2.40, 'CONCRETO', TRUE, TRUE, FALSE, TRUE, TRUE,
 ST_SetSRID(ST_MakeLine(ST_MakePoint(-46.4080, -23.9926), ST_MakePoint(-46.4070, -23.9930)), 4326)),

-- Atalho pela praca: mais direto, porem em terra batida e com um degrau na entrada.
('atalho-praca', 'Praça Central', 'av-2', 'av-4', 2.0, 1.60, 'TERRA_COMPACTADA', FALSE, TRUE, FALSE, FALSE, TRUE,
 ST_SetSRID(ST_MakeLine(ST_MakePoint(-46.4100, -23.9922), ST_MakePoint(-46.4080, -23.9926)), 4326)),

-- Travessias com guia rebaixada ligando a avenida a rua paralela.
('cruz-2', 'Travessia Rua Paraguai', 'av-2', 'par-2', 0.0, 3.00, 'ASFALTO', TRUE, TRUE, TRUE, TRUE, TRUE,
 ST_SetSRID(ST_MakeLine(ST_MakePoint(-46.4100, -23.9922), ST_MakePoint(-46.4100, -23.9932)), 4326)),

('cruz-3', 'Travessia Rua Uruguai', 'av-3', 'par-3', 0.0, 3.00, 'ASFALTO', FALSE, TRUE, TRUE, TRUE, TRUE,
 ST_SetSRID(ST_MakeLine(ST_MakePoint(-46.4090, -23.9924), ST_MakePoint(-46.4090, -23.9934)), 4326)),

('cruz-4', 'Travessia Rua Argentina', 'av-4', 'par-4', 0.0, 3.00, 'ASFALTO', TRUE, TRUE, TRUE, TRUE, TRUE,
 ST_SetSRID(ST_MakeLine(ST_MakePoint(-46.4080, -23.9926), ST_MakePoint(-46.4080, -23.9936)), 4326)),

-- Rua paralela: piso intertravado, um pouco mais lento, porem livre de barreiras.
('par-2-3', 'Rua Paraguai', 'par-2', 'par-3', 0.5, 1.80, 'PISO_INTERTRAVADO', FALSE, TRUE, FALSE, TRUE, TRUE,
 ST_SetSRID(ST_MakeLine(ST_MakePoint(-46.4100, -23.9932), ST_MakePoint(-46.4090, -23.9934)), 4326)),

('par-3-4', 'Rua Paraguai', 'par-3', 'par-4', 0.5, 1.80, 'PISO_INTERTRAVADO', FALSE, TRUE, FALSE, TRUE, TRUE,
 ST_SetSRID(ST_MakeLine(ST_MakePoint(-46.4090, -23.9934), ST_MakePoint(-46.4080, -23.9936)), 4326));

-- ---------------------------------------------------------------------------
-- Obstaculos
-- ---------------------------------------------------------------------------
-- Apenas os VALIDADOS entram no calculo de rota. O relato pendente fica visivel no
-- mapa e na fila de curadoria, mas ainda nao desvia o trajeto de ninguem.

INSERT INTO obstaculo (id, tipo, severidade, localizacao, descricao, segmento_via_id,
                       reportado_por, reportado_em, status, avaliado_em) VALUES
('b2000000-0000-4000-8000-000000000001', 'OBRA', 'MODERADA',
 ST_SetSRID(ST_MakePoint(-46.4085, -23.9925), 4326),
 'Tapume de obra ocupa a calçada e deixa menos de um metro de passagem.',
 'av-3-4', 'equipe-mapeamento', now() - INTERVAL '10 days', 'VALIDADO', now() - INTERVAL '9 days'),

('b2000000-0000-4000-8000-000000000002', 'DEGRAU', 'MODERADA',
 ST_SetSRID(ST_MakePoint(-46.4099, -23.9923), 4326),
 'Degrau de cerca de 15 cm na entrada da praça, sem rampa alternativa.',
 'atalho-praca', 'equipe-mapeamento', now() - INTERVAL '10 days', 'VALIDADO', now() - INTERVAL '9 days'),

-- Ainda pendente: embora um degrau barre qualquer cadeira de rodas, este relato nao
-- desvia rota nenhuma enquanto a curadoria nao o confirmar.
('b2000000-0000-4000-8000-000000000003', 'DEGRAU', 'MODERADA',
 ST_SetSRID(ST_MakePoint(-46.4085, -23.9935), 4326),
 'Parece haver um degrau no meio da calçada; relato ainda não conferido em campo.',
 'par-3-4', 'anônimo', now() - INTERVAL '2 days', 'PENDENTE', NULL);
