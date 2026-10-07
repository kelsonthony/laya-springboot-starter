# Contribuir

Obrigado por contribuir com a integração da comunidade. Abra uma issue para discutir alterações maiores e envie pull requests com mudanças focadas.

## Preparar e testar

Java 17+ é obrigatório. O Maven Wrapper baixa o Maven automaticamente.

```bash
./mvnw clean install
./mvnw -f examples/support-triage/pom.xml clean verify
```

Os testes usam HTTP simulado; não precisam de chave nem de modelos. Para testar inferência real, siga o README para executar o servidor oficial, inicie o exemplo e rode `python3 scripts/smoke.py`. Informe separadamente os testes simulados e os testes de inferência na descrição do PR.

Mantenha a configuração automática sem chamadas de rede no startup. Preserve o builder HTTP do Spring Boot e o contrato oficial de `/v1/systemone`. Não adicione retries implícitos. Atualize os dois READMEs quando a API pública mudar. Nunca inclua chaves, arquivos `.env`, pesos de modelos ou dados pessoais em commits.

Use commits convencionais, como `feat(client): adiciona recurso` ou `fix(autoconfigure): corrige configuração`. As contribuições são disponibilizadas sob a licença Apache-2.0 deste projeto.
